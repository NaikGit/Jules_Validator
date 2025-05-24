package com.example.validation_service.dto;

import java.util.Objects;

public class Payer {
    private String name;
    private String id;

    public Payer() {
    }

    public Payer(String name, String id) {
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
        Payer payer = (Payer) o;
        return Objects.equals(name, payer.name) &&
               Objects.equals(id, payer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    @Override
    public String toString() {
        return "Payer{" +
               "name='" + name + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
