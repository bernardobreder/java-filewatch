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

	private int pollingInterval;

	private Timer fileWatcher;

	private final Map<File, Long> timestamps = new HashMap<>();

	private Set<File> processedFiles = new HashSet<>();

	private final Predicate<Path> filter;

	public FileWatcher(Path path, Predicate<Path> filter, Consumer<FileWatcherChanged> fileConsumer) throws IOException {
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
					Set<Path> addedPaths = new HashSet<>();
					Set<Path> changedPaths = new HashSet<>();
					Set<File> files = files(path);
					Set<File> changedFiles = new HashSet<>();
					for (File file : files) {
						Long timestamp = timestamps.get(file);
						long lastModified = file.lastModified();
						if (timestamp == null || lastModified > timestamp) {
							if (timestamp == null) {
								addedPaths.add(file.toPath());
							} else {
								changedPaths.add(file.toPath());
							}
							timestamps.put(file, lastModified);
							changedFiles.add(file);
						}
					}
					processedFiles.removeAll(files);
					Set<Path> removedPaths = processedFiles.stream().map(e -> e.toPath()).collect(Collectors.toSet());
					processedFiles = files;
					if (!changedFiles.isEmpty()) {
						fileConsumer.accept(new FileWatcherChanged(addedPaths, changedPaths, removedPaths));
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

	public static class FileWatcherChanged {

		public final Set<Path> addedPaths;

		public final Set<Path> changedPaths;

		public final Set<Path> removedPaths;

		public FileWatcherChanged(Set<Path> addedPaths, Set<Path> changedPaths, Set<Path> removedPaths) {
			super();
			this.addedPaths = addedPaths;
			this.changedPaths = changedPaths;
			this.removedPaths = removedPaths;
		}

	}

}