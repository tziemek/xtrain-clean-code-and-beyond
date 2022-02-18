import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SegmentationService {

	public InputStream GetSegmentData(int fid, long firstIndex, long lastIndex)
			throws IOException, URISyntaxException {
		return GetSegmentData(fid, firstIndex, lastIndex, 5000);
	}

	public InputStream GetSegmentData(int fid, long x1, long x2, long points)
			throws IOException, URISyntaxException {
		String filename;

		try {
			filename = getCsvFileName(fid);
		} catch (Exception ex) {
			throw new IllegalArgumentException("unknown file id");
		}

		long fileLines = GetSampleCountByFileId(fid);

		if (points < 0) {
			throw new IllegalArgumentException("points has to be non-negative");
		}

		// set min value
		if (x1 == 0) {
			x1 = 1;
		}

		// set max value
		if (x2 == 0) {
			x2 = fileLines;
		}

		if (x2 < x1) {
			throw new IllegalArgumentException("x2 is smaller than x1");
		}

		long originalSegmentSize = (x2 - x1 + 1);
		String outputFile;

		if (x1 == 1 && x2 == fileLines && originalSegmentSize <= points) {
			// take original file
			outputFile = filename;
		} else {
			String[] tmp = Paths.get(filename).getFileName().toString().split("\\.");
			String extension = tmp[tmp.length - 1];
			outputFile = Paths.get(filename).getParent().toString() + File.separator
					+ Paths.get(filename).getFileName().toString().replaceFirst("[.][^.]+$", "") + "_"
					+ points + "_" + x1 + "_" + x2 + "." + extension;
		}

		if (!new File(outputFile).exists()) {
			executeCopyProcess(points, x1, x2, filename, outputFile);
		}

		return Files.newInputStream(Paths.get(outputFile));
	}

	private String getCsvFileName(int fileId) throws Exception {
		// when fileId is less than 0
		if (fileId < 0) {
			// throw an exception
			throw new Exception();
		}
		String basePath = new File(
				SegmentationService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
						.getParent();
		return Paths.get(basePath, fileId + ".csv").toString();
	}

	private void executeCopyProcess(long desiredLineCount, long startLineOffset, long endlineOffset, String sourcePath, String targetPath) throws IOException {
		try (BufferedReader inputReader = new BufferedReader(new FileReader(sourcePath))) {
			try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(targetPath))) {
				// write header
				outputWriter.write(inputReader.readLine());
				outputWriter.newLine();

				long expectedLineCount = endlineOffset - startLineOffset;
				if (expectedLineCount > desiredLineCount) {
					double stepSize = expectedLineCount / (double) desiredLineCount;
					CopyDownSampledSegment(inputReader, outputWriter, startLineOffset, endlineOffset, stepSize);
				} // if
				else {
					CopySegment(inputReader, outputWriter, startLineOffset, endlineOffset);
				} // else
				outputWriter.flush();
				outputWriter.close();
			} // using outputWriter
		} // using inputWriter
	} // ExecuteCopyProcess

	private static void CopyDownSampledSegment(BufferedReader inputReader, BufferedWriter outputWriter,
			double start, double end, double stepSize) throws IOException {
		double targetOffset = start;

		for (long readLineIndex = 0; targetOffset < end; readLineIndex++) {
			String line = inputReader.readLine();
			if (line == null || line.length() == 0) {
				break;
			} // if
			if (readLineIndex >= targetOffset) {
				outputWriter.write(line);
				outputWriter.newLine();
				targetOffset += stepSize;
			} // if
		} // while
	} // CopySegement

	private static void CopySegment(BufferedReader inputReader, BufferedWriter outputWriter, long begin,
			long endLineOffset) throws IOException {
		for (long lineIndex = 0; lineIndex < endLineOffset; lineIndex++) {
			String line = inputReader.readLine();
			if (line == null || line.length() == 0) {
				break;
			} // if
			if (lineIndex >= begin) {
				outputWriter.write(line);
				outputWriter.newLine();
			} // if
		} // for
	} // CopySegemnt

	private long GetSampleCountByFileId(int id) throws URISyntaxException, FileNotFoundException, IOException {
		String basePath = new File(
				SegmentationService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
						.getParent();
		try (LineNumberReader reader = new LineNumberReader(new FileReader(new File(basePath, id + ".csv")))) {
			reader.skip(Long.MAX_VALUE);
			// first row is header row and does not count but getLineNumber is 0 based
			return reader.getLineNumber();
		}
	}
}
