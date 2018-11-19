package com.almejo.osom.cpu;

import java.util.HashMap;

public class OperationTable {
	private HashMap<Integer, Operation> operations = new HashMap<>();

	private HashMap<Integer, Operation> operationsCB = new HashMap<>();

	public Operation getOperation(int operationCode) throws OperationNotFoundException {
		Operation operation;
		if (operations.containsKey(operationCode)) {
			operation = operations.get(operationCode);
		} else {
			throw new OperationNotFoundException("code not found 0x" + Integer.toHexString(operationCode));
		}
		return operation;
	}

	public Operation getOperationCB(int operationCode) throws OperationNotFoundException {
		Operation operation;
		if (operationsCB.containsKey(operationCode)) {
			operation = operationsCB.get(operationCode);
		} else {
			throw new OperationNotFoundException("code not found 0xcb 0x" + Integer.toHexString(operationCode));
		}
		return operation;
	}

	void addOpcode(Operation operation) {
		if (operation instanceof OperationCB) {
			operationsCB.put(operation.code, operation);
			return;
		}
		operations.put(operation.code, operation);
	}
}
