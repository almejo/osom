package com.almejo.osom;

import com.almejo.osom.cpu.Operation;
import com.almejo.osom.cpu.OperationNotFoundException;
import com.almejo.osom.cpu.OperationTable;
import com.almejo.osom.memory.Cartridge;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

class CodeViewer extends Component {
	private static int PREFIX_CB = 0xcb;

	CodeViewer(OperationTable operationTable, Cartridge cartridge, List<String> strings) {
		HashMap<Integer, String> data = parseData(strings);
		int i = 0x0;
		while (i < cartridge.getBytes().length) {
			if (data.containsKey(i)) {
				System.out.println(Integer.toHexString(i) + " .db 0x" + data.get(i));
				i++;
				continue;
			}
			int operationCode = cartridge.getByte(i);
			// System.out.println(aByte);
			Operation operation;
			try {
//				if (operationCode == 0xff) {
//					System.out.println(".db 0xff");
//					i++;
//					continue;
//				}
				if (operationCode == PREFIX_CB) {
					operationCode = cartridge.getByte(++i);
					operation = operationTable.getOperationCB(operationCode);
					System.out.println(Integer.toHexString(i - 1) + " 0cb " + Integer.toHexString(operationCode) + " " + operation.decoded(cartridge.getBytes(), i));
				} else {
					operation = operationTable.getOperation(operationCode);
					System.out.println(Integer.toHexString(i) + " " + Integer.toHexString(operationCode) + " " + operation.decoded(cartridge.getBytes(), i));
				}
				i += operation.getLength();
			} catch (OperationNotFoundException e) {
				e.printStackTrace();
				System.out.println("at 0x" + Integer.toHexString(i));
				System.exit(0);
			}
		}
	}

	private HashMap parseData(List<String> strings) {
		HashMap<Integer, String> map = new HashMap<>();
		strings.forEach(string -> {
			if (string.trim().length() > 0) {
				String[] data = string.trim().split(" ");
				int address = Integer.decode("0x" + data[0].replaceAll("^0*", ""));

				if (data.length > 1) {
					for (int i = +1; i < data.length; i++) {
						map.put(address, data[i]);
						address++;
					}
				}
			}
		});
		return map;
	}
}
