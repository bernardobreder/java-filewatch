package filewatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileWatcher {

	private Path path;

	private int pollingInterval;

	private Timer fileWatcher;

	private long lastReadTimeStamp = 0L;

	private final Map<File, Long> timestamps = new HashMap<>();

	private Set<File> processedFiles = new HashSet<>();

	private final Predicate<Path> filter;

	public FileWatcher(Path path, Predicate<Path> filter, Consumer<Set<File>> fileConsumer) throws IOException {
		this.path = path;
		this.filter = filter;
		pollingInterval = 100;
		for (File file : files(path)) {
			processedFiles.add(file);
			timestamps.put(file, file.lastModified());
		}
		fileWatcher = new Timer();
		fileWatcher.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					Set<File> files = files(path);
					Set<File> changedFiles = new HashSet<>();
					for (File file : files) {
						Long timestamp = timestamps.get(file);
						long lastModified = file.lastModified();
						if (timestamp == null || lastModified > timestamp) {
							timestamps.put(file, lastModified);
							changedFiles.add(file);
						}
					}
					processedFiles.removeAll(files);
					changedFiles.addAll(processedFiles);
					processedFiles = files;
					if (!changedFiles.isEmpty()) {
						fileConsumer.accept(changedFiles);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}, 0, pollingInterval);
	}

	protected Set<File> files(Path path) throws IOException {
		return Files.walk(path) //
				.filter(filter) //
				.map(e -> e.toFile()) //
				.collect(Collectors.toSet());
	}

}