package org.example;

public enum SensorType {
    TEMPERATURE("TEMP", -30.0, 50.0, "°C"),
    OXYGEN("O2", 15.0, 28.0, "%"),
    AIR_PRESSURE("PRESSURE", 500.0, 1400.0, "hPa"),
    CO2("CO2", 1800.0, 2200.0, "ppm");

    private final String type;
    private final double minValue;
    private final double maxValue;
    private final String unit;

    SensorType(String type, double minValue, double maxValue, String unit) {
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
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

    public String getUnit() {
        return unit;
    }

    public static SensorType fromMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Sensor type is missing");
        }

        String normalized = messageType.trim().toUpperCase();
        return switch (normalized) {
            case "TEMP", "TEMPERATURE" -> TEMPERATURE;
            case "O2", "OXYGEN" -> OXYGEN;
            case "PRESSURE", "TRYK", "AIR_PRESSURE" -> AIR_PRESSURE;
            case "CO2" -> CO2;
            default -> throw new IllegalArgumentException("Unknown sensor type: " + messageType);
        };
    }

}
