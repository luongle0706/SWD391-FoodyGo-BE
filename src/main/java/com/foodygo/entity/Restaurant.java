package com.foodygo.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "restaurant")
public class Restaurant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String name;

    String phone;

    String email;

    String address;

    String image;

    @OneToOne
    User owner;

    @OneToMany(mappedBy = "restaurant")
    List<Wallet> wallets;

    @Builder.Default
    boolean available = true;

    @OneToMany(mappedBy = "restaurant")
    List<Order> orders;

    @OneToMany(mappedBy = "restaurant")
    List<Product> products;

    @OneToMany(mappedBy = "restaurant")
    List<Category> categories;
}
