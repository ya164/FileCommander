package com.filecommander.model;

public class OperationHistory {
    private int id;
    private String operationType;
    private String description;
    private String executedAt;
    private String status;

    public OperationHistory(int id, String operationType, String description,
                            String executedAt, String status) {
        this.id = id;
        this.operationType = operationType;
        this.description = description;
        this.executedAt = executedAt;
        this.status = status;
    }

    public int getId() { return id; }
    public String getOperationType() { return operationType; }
    public String getDescription() { return description; }
    public String getExecutedAt() { return executedAt; }
    public String getStatus() { return status; }
}