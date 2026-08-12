package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyDocument;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code getDocumentOrThrow} is what PropertyController#downloadDocument
 * relies on now that documents are no longer served as public static files -
 * a document id from one property must never resolve under a different one.
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceDocumentAccessTest {

    @Mock
    private PropertyDocumentRepository propertyDocumentRepository;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void getDocumentOrThrow_succeedsWhenDocumentBelongsToTheGivenProperty() {
        Property property = Property.builder().name("P1").status(PropertyStatus.ACTIVE).build();
        property.setId(UUID.randomUUID());
        PropertyDocument document = PropertyDocument.builder().property(property).fileName("f.pdf").build();
        document.setId(UUID.randomUUID());

        when(propertyDocumentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        PropertyDocument result = propertyService.getDocumentOrThrow(property.getId(), document.getId());

        assertThat(result).isEqualTo(document);
    }

    @Test
    void getDocumentOrThrow_rejectsWhenDocumentBelongsToADifferentProperty() {
        Property property = Property.builder().name("P1").status(PropertyStatus.ACTIVE).build();
        property.setId(UUID.randomUUID());
        Property otherProperty = Property.builder().name("P2").status(PropertyStatus.ACTIVE).build();
        otherProperty.setId(UUID.randomUUID());
        PropertyDocument document = PropertyDocument.builder().property(otherProperty).fileName("f.pdf").build();
        document.setId(UUID.randomUUID());

        when(propertyDocumentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> propertyService.getDocumentOrThrow(property.getId(), document.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
