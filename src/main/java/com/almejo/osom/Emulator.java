package com.almejo.osom;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Emulator {

	public void run(String file) throws IOException {
		Path path = Paths.get(file);
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU();
		Cartridge cartridge = new Cartridge(bytes);
		System.out.println(cartridge);
		mmu.addCartridge(cartridge);
		Z80Cpu cpu = new Z80Cpu();
		cpu.setMmu(mmu);
		cpu.reset();

		JTextArea textArea = new JTextArea();
		textArea.setText(cartridge.toString());
		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));

		JFrame frame = new JFrame();
		frame.getContentPane().add(new JScrollPane(textArea));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		//noinspection InfiniteLoopStatement
		while (true) {
			cpu.execute();
		}
	}
}