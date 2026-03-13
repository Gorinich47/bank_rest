package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles_old")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

//    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<User> users;

}