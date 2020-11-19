package com.tufusi.demo.event;

/**
 * Created by LeoCheung on 2020/11/18.
 *
 * @description
 */
public class PersonEvent {

    private String name;
    private int age;

    public PersonEvent(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "PersonEvent{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
} 