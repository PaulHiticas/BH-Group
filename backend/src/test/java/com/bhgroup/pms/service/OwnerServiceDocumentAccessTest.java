package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.Expense;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyDocument;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The /uploads/** static path is no longer public for property documents or
 * expense receipts (see SecurityConfig) - these lookups are what actually
 * gate access now, so an owner can never read another owner's documents by
 * guessing a property/document/expense id.
 */
@ExtendWith(MockitoExtension.class)
class OwnerServiceDocumentAccessTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PropertyDocumentRepository propertyDocumentRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private OwnerService ownerService;

    private User owner(String suffix) {
        User user = User.builder()
                .email("owner-" + suffix + "@example.com")
                .passwordHash("hash")
                .firstName("Owner")
                .lastName(suffix)
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Property propertyOwnedBy(User owner) {
        Property property = Property.builder()
                .name("Test property")
                .status(PropertyStatus.ACTIVE)
                .owner(owner)
                .build();
        property.setId(UUID.randomUUID());
        return property;
    }

    @Test
    void getMyDocumentOrThrow_succeedsWhenOwnerOwnsTheProperty() {
        User owner = owner("A");
        Property property = propertyOwnedBy(owner);
        PropertyDocument document = PropertyDocument.builder().property(property).fileName("contract.pdf").build();
        document.setId(UUID.randomUUID());

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(propertyDocumentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        PropertyDocument result = ownerService.getMyDocumentOrThrow(owner.getId(), property.getId(), document.getId());

        assertThat(result).isEqualTo(document);
    }

    @Test
    void getMyDocumentOrThrow_rejectsWhenPropertyBelongsToAnotherOwner() {
        User actualOwner = owner("A");
        User attacker = owner("B");
        Property property = propertyOwnedBy(actualOwner);
        UUID documentId = UUID.randomUUID();

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));

        assertThatThrownBy(() -> ownerService.getMyDocumentOrThrow(attacker.getId(), property.getId(), documentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyDocumentOrThrow_rejectsWhenDocumentBelongsToADifferentProperty() {
        User owner = owner("A");
        Property property = propertyOwnedBy(owner);
        Property otherProperty = propertyOwnedBy(owner);
        PropertyDocument documentOnOtherProperty = PropertyDocument.builder()
                .property(otherProperty).fileName("other.pdf").build();
        documentOnOtherProperty.setId(UUID.randomUUID());

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(propertyDocumentRepository.findById(documentOnOtherProperty.getId()))
                .thenReturn(Optional.of(documentOnOtherProperty));

        assertThatThrownBy(() -> ownerService.getMyDocumentOrThrow(
                owner.getId(), property.getId(), documentOnOtherProperty.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyExpenseOrThrow_rejectsWhenExpensePropertyBelongsToAnotherOwner() {
        User actualOwner = owner("A");
        User attacker = owner("B");
        Property property = propertyOwnedBy(actualOwner);
        Expense expense = Expense.builder().property(property).build();
        expense.setId(UUID.randomUUID());

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> ownerService.getMyExpenseOrThrow(attacker.getId(), expense.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyExpenseOrThrow_succeedsWhenOwnerOwnsTheExpensesProperty() {
        User owner = owner("A");
        Property property = propertyOwnedBy(owner);
        Expense expense = Expense.builder().property(property).build();
        expense.setId(UUID.randomUUID());

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        Expense result = ownerService.getMyExpenseOrThrow(owner.getId(), expense.getId());

        assertThat(result).isEqualTo(expense);
    }

    @Test
    void loadReceiptResource_rejectsWhenNoReceiptUploaded() {
        Expense expense = Expense.builder().build();

        assertThatThrownBy(() -> ownerService.loadReceiptResource(expense))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
