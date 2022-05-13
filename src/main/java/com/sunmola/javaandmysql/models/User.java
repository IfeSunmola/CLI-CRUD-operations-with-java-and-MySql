package com.sunmola.javaandmysql.models;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String username;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    @Transient // age should not be a column in the database. It will be calculated in getAge()
    private int age;
    private String gender;
    @Transient
    private LocalDateTime dateOfRegistration;

    public User() {
    }

    public User(String phoneNumber, String name, LocalDate dateOfBirth, int age, String gender) {
        this.phoneNumber = phoneNumber;
        this.username = name;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.gender = gender;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getAge() {
        return Period.between(this.dateOfBirth, LocalDate.now()).getYears();
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDateTime getDateOfRegistration() {
        return LocalDateTime.now();
    }
}
