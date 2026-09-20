package org.example;

public enum SensorType {
    TEMPERATURE("TEMP", -50.0, 80.0),
    OXYGEN("O2", 0.0, 100.0),
    AIR_PRESSURE("PRESSURE", 950.0, 1050.0),
    CO2("CO2", 300.0, 5000.0);

    private final String type;
    private final double minValue;
    private final double maxValue;

    SensorType(String type, double minValue, double maxValue) {
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getType() {
        return type;
    }

    public double getMinValue(){
        return minValue;
    }

    public double getMaxValue(){
        return maxValue;
    }

}
