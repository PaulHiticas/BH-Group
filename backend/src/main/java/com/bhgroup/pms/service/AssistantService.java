package com.bhgroup.pms.service;

import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.dto.assistant.AssistantChatResponse;
import com.bhgroup.pms.dto.assistant.AssistantMessageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * FAQ-only chat assistant backed by the Anthropic Messages API. V1 is
 * intentionally stateless (the caller resends the full history each turn)
 * and answers strictly from the system prompt's FAQ content - no live
 * reservation/account data is ever read or exposed here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final RestClient anthropicRestClient;
    private final AppProperties appProperties;

    private static final String OUTAGE_FALLBACK_TEMPLATE =
            "Nu pot prelua acum răspunsul asistentului virtual. Te rugăm să ne contactezi %s și revenim cât mai repede.";

    // Machine-readable escalation signal: the model appends this exact
    // marker on its own line whenever it defers to a human (the same two
    // cases the system prompt already tells it to defer on), so the
    // handoff flow (V2) can detect "the AI couldn't help" without fragile
    // text-matching on the visible reply. Stripped before the text is
    // ever returned to the caller.
    private static final String NEEDS_HUMAN_MARKER = "[[NEEDS_HUMAN]]";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Ești asistentul virtual al BH Group, o platformă de administrare și rezervare \
            de proprietăți de cazare pe termen scurt.

            Reguli stricte pe care trebuie să le respecți întotdeauna:
            - Răspunde DOAR la întrebări generale despre platformă și servicii (FAQ), \
            folosind STRICT informațiile de mai jos. Nu inventa politici, prețuri sau \
            detalii care nu apar explicit aici.
            - Există un mecanism real prin care un coleg din echipă preia conversația \
            chiar în acest chat, imediat. NU spune NICIODATĂ că nu poți conecta clientul \
            cu o persoană, că ești doar un asistent virtual și nu poți face asta, sau \
            ceva similar - e o afirmație falsă, mecanismul chiar există și funcționează.
            - Dacă clientul cere EXPLICIT să vorbească cu un om / o persoană / cineva / \
            echipa / un operator (română sau engleză), NU pune întrebări suplimentare \
            (ex. „care e problema?"), NU enumera opțiuni de contact și NU nega că poți \
            face asta - răspunde DOAR cu o propoziție scurtă și pozitivă, de tipul \
            „Te conectez imediat cu un coleg din echipă.", apoi încheie direct cu \
            marcajul de mai jos.
            - Dacă întrebarea este despre o rezervare anume, despre datele personale ale \
            unui client, sau clientul vrea să creeze/modifice o rezervare acum, NU încerca \
            să rezolvi tu - spune-i politicos că un coleg din echipă revine către el, și \
            oferă-i datele de contact de mai jos.
            - Dacă nu știi răspunsul sau informația nu apare mai jos, spune sincer că nu \
            ești sigur și trimite clientul la contact - nu ghici și nu inventa.
            - În oricare din cele trei cazuri de mai sus (cerere explicită de a vorbi cu \
            un om, SAU rezervare anume/date personale, SAU nu știi răspunsul), încheie \
            răspunsul tău EXACT cu marcajul [[NEEDS_HUMAN]] pe propria linie, la final. \
            NU folosi acest marcaj pentru un răspuns FAQ normal la care ai reușit să \
            răspunzi din informațiile de mai jos.
            - Răspunde în limba în care scrie clientul (română sau engleză). Fii concis \
            (2-4 propoziții).

            Informații despre BH Group:
            1. Check-in / check-out: ora de check-in și check-out variază pe proprietate - \
            apare clar pe pagina fiecărui anunț, înainte de trimiterea cererii. Unele \
            proprietăți au check-in autonom cu smart lock; altele au check-in cu personal.
            2. Anulare rezervare: fiecare proprietate are propria politică de anulare \
            (flexibilă, moderată, strictă sau nerambursabilă), afișată clar pe pagina \
            anunțului, înainte de trimiterea cererii.
            3. Facilități (inclusiv parcare): fiecare proprietate are propriul set de \
            facilități, afișat pe pagina anunțului - poți filtra din pagina de căutare \
            după facilitățile de care ai nevoie (parcare, animale de companie etc.).
            4. Animale de companie: unele proprietăți acceptă animale - poți filtra după \
            această facilitate în căutare.
            5. Comision / cost administrare (pentru proprietari): depinde de locație, \
            tipul proprietății și serviciile incluse - se stabilește transparent la prima \
            discuție, fără costuri ascunse.
            6. Curățenie: fiecare sejur include curățenie și lenjerie/prosoape curate; \
            taxa de curățenie (dacă se aplică) apare defalcat, separat, în cererea de \
            rezervare.
            7. Plată: nu se percepe nicio plată la trimiterea cererii de rezervare - \
            echipa confirmă manual cererea, iar modalitatea de plată se stabilește cu \
            echipa după confirmare.
            8. Cum rezerv: căutarea și rezervarea se fac din pagina „Vezi apartamente" - \
            alegi orașul, datele și numărul de oaspeți.
            9. Locație: proprietățile sunt în orașele afișate în căutare; adresa exactă e \
            trimisă doar după confirmarea rezervării.
            10. Listarea unei proprietăți: proprietarii pot lista o proprietate prin \
            formularul „Listează-ți proprietatea", lăsând datele de contact; echipa revine \
            cu o estimare de venit.
            11. Contact: %s
            """;

    public AssistantChatResponse chat(List<AssistantMessageRequest> history) {
        String contactFallback = buildContactFallback();

        String apiKey = appProperties.getAssistant().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY is not configured - returning the contact fallback instead of calling the API");
            // The bot genuinely couldn't help at all here, so this always
            // counts as a needs-human signal too.
            return new AssistantChatResponse(OUTAGE_FALLBACK_TEMPLATE.formatted(contactFallback), true);
        }

        try {
            AnthropicResponse response = anthropicRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .body(new AnthropicRequest(
                            appProperties.getAssistant().getModel(),
                            appProperties.getAssistant().getMaxTokens(),
                            SYSTEM_PROMPT_TEMPLATE.formatted(contactFallback),
                            trimHistory(history).stream()
                                    .map(m -> new AnthropicMessage(m.role(), m.content()))
                                    .toList()
                    ))
                    .retrieve()
                    .body(AnthropicResponse.class);

            String answer = extractText(response);
            if (answer == null || answer.isBlank()) {
                return new AssistantChatResponse(OUTAGE_FALLBACK_TEMPLATE.formatted(contactFallback), true);
            }

            boolean needsHuman = answer.contains(NEEDS_HUMAN_MARKER);
            String cleaned = needsHuman ? answer.replace(NEEDS_HUMAN_MARKER, "").trim() : answer;
            return new AssistantChatResponse(cleaned, needsHuman);
        } catch (Exception ex) {
            log.error("Anthropic API call failed - returning the contact fallback", ex);
            return new AssistantChatResponse(OUTAGE_FALLBACK_TEMPLATE.formatted(contactFallback), true);
        }
    }

    // Defense in depth: AssistantChatRequest already caps the history at 20
    // messages via @Size, but this keeps AssistantService safe against any
    // other future caller that skips DTO validation.
    private List<AssistantMessageRequest> trimHistory(List<AssistantMessageRequest> history) {
        int max = appProperties.getAssistant().getMaxHistoryMessages();
        return history.size() <= max ? history : history.subList(history.size() - max, history.size());
    }

    private String buildContactFallback() {
        String email = appProperties.getContact().getEmail();
        String phone = appProperties.getContact().getPhone();
        String joined = Stream.of(email, phone)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" sau la "));
        return joined.isBlank() ? "prin formularul de contact de pe pagina „Pentru proprietari”" : joined;
    }

    private String extractText(AnthropicResponse response) {
        if (response == null || response.content() == null || response.content().isEmpty()) {
            return null;
        }
        return response.content().get(0).text();
    }

    // Package-private (not private) so AssistantServiceTest can stub the
    // exact response type RestClient.ResponseSpec#body(Class) deserializes to.
    record AnthropicMessage(String role, String content) {
    }

    record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<AnthropicMessage> messages
    ) {
    }

    record AnthropicContentBlock(String type, String text) {
    }

    record AnthropicResponse(List<AnthropicContentBlock> content) {
    }
}
