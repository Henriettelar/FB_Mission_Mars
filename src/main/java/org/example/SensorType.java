package org.example;

public enum SensorType {
    TEMPERATURE("TEMP", -30.0, 50.0, "°C", -15.0, 35.0),
    OXYGEN("O2", 15.0, 28.0, "%", 19.0, 23.0),
    AIR_PRESSURE("PRESSURE", 500.0, 1400.0, "hPa", 800.0, 1100.0),
    CO2("CO2", 1800.0, 2200.0, "ppm", 0.0, 2000.0);

    private final String type;
    private final double minValue;
    private final double maxValue;
    private final String unit;
    private final double minThreshold;
    private final double maxThreshold;

    SensorType(String type, double minValue, double maxValue, String unit, double minThreshold, double maxThreshold) {
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
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

    public double getMinThreshold() {
        return minThreshold;
    }

    public double getMaxThreshold() {
        return maxThreshold;
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
