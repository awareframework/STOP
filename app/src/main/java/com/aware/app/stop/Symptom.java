package com.aware.app.stop;

/*
 * Class for Symptom object
 */

public class Symptom {

    private String name;
    private String description;
    private boolean isDescriptionShown;

    public Symptom(String name, String description, boolean isDescriptionShown1) {
        this.name = name;
        this.description = description;
        isDescriptionShown = isDescriptionShown1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDescriptionShown() {
        return isDescriptionShown;
    }

    public void setDescriptionShown(boolean descriptionShown) {
        isDescriptionShown = descriptionShown;
    }
}
