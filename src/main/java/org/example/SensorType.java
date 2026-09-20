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
