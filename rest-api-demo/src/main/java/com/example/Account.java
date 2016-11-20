package com.example;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * Created by sun on 2016/11/20.
 */
@Entity
@Table(name = "t_account")
public class Account { // 1

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id; // 2

    @Column(unique = true, nullable = false, length = 20)
    private String username; // 3

    @JsonIgnore
    private String password;

    private String email;
    private String firstName;
    private String lashName;
    private Integer age;
    private Byte gender;

    public Account() {
    }

    public Account(String username, String password, String email, String firstName, String lashName, Integer age, Byte gender) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lashName = lashName;
        this.age = age;
        this.gender = gender;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLashName() {
        return lashName;
    }

    public void setLashName(String lashName) {
        this.lashName = lashName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lashName='" + lashName + '\'' +
                ", age=" + age +
                ", gender=" + gender +
                '}';
    }
}
