package com.shop.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * An address owned by a user. Lives in this service's database only — no other
 * service reads this table; if they need an address they ask user-service.
 */
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String postcode;

    @Column(nullable = false)
    private String country;

    protected Address() { // JPA
    }

    public Address(String line1, String line2, String city, String postcode, String country) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.postcode = postcode;
        this.country = country;
    }

    public UUID getId() {
        return id;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCountry() {
        return country;
    }
}
