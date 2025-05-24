package com.example.validation_service.dto;

import java.util.Objects;

public class Payee {
    private String name;
    private String id;

    public Payee() {
    }

    public Payee(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payee payee = (Payee) o;
        return Objects.equals(name, payee.name) &&
               Objects.equals(id, payee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    @Override
    public String toString() {
        return "Payee{" +
               "name='" + name + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
