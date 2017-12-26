package com.almejo.osom;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.GPU;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Emulator {

	public void run(boolean bootBios, String file) throws IOException {
		Path path = Paths.get(file);
		GPU gpu = new GPU();
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU(bootBios, gpu);
		Cartridge cartridge = new Cartridge(bytes);
		mmu.addCartridge(cartridge);
		Z80Cpu cpu = new Z80Cpu(mmu);
		cpu.reset(bootBios);


//		JTextArea textArea = new JTextArea();
//		textArea.setText(cartridge.toString());
//		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
//
//		JFrame frame = new JFrame();
//		frame.getContentPane().add(new JScrollPane(textArea));
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.pack();
//		frame.setVisible(true);
		int cyclesPerSecond = 4194304;
		int i = 0;
		//noinspection InfiniteLoopStatement
		while (true) {
			System.out.print(i + "--> ");
			int oldCycles = cpu.clock.getT();
			cpu.execute();
			int cycles = cpu.clock.getT() - oldCycles;
			cpu.updateTimers(cycles);
			gpu.update(cycles);
			i++;
		}
	}
}
