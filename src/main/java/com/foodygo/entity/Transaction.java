package com.foodygo.entity;

import com.foodygo.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String description;

    LocalDateTime time = LocalDateTime.now();

    Double amount;

    Double remaining;

    @Enumerated(EnumType.STRING)
    TransactionType type;

    @ManyToOne
    Order order;

    @ManyToOne
    Wallet wallet;

    @ManyToOne
    Deposit deposit;
}
