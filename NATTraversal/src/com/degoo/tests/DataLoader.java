package com.degoo.tests;

import com.degoo.util.io.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * This class will load file from specified dir for test.
 */
public class DataLoader {
	private final Map<String, String> fileNamePathMap = new HashMap<>();

	/**
	 * Read specified file as binary data from Dir.
	 */
	public byte[] readData(String fileName, String fileDir) throws IOException {
		if (fileNamePathMap.isEmpty() || fileNamePathMap.get(fileName) == null) {
			loadAllFilesPath(fileDir);
		}

		Path fileToRead = Paths.get(fileNamePathMap.get(fileName));
		return FileUtil.readAllBytesFast(fileToRead);
	}

	public long getFileSize(String fileName, String fileDir) throws Exception {
		if (fileNamePathMap.isEmpty() || fileNamePathMap.get(fileName) == null) {
			loadAllFilesPath(fileDir);
		}

		String path = fileNamePathMap.get(fileName);
		if (path == null) {
			throw new FileNotFoundException();
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		return file.length();
	}

	private void refresh() {
		fileNamePathMap.clear();
	}


	private void loadAllFilesPath(String dir) throws FileNotFoundException {
		refresh();

		File file = new File(dir);
		if (!file.exists()) {
			throw new FileNotFoundException("Dir: " + dir + " not found");
		}

		for (String name : file.list()) {
			String path = dir + name;
			String nameWithoutSuffix = name.replaceAll(".txt", "");
			fileNamePathMap.put(nameWithoutSuffix, path);
		}
	}
}
