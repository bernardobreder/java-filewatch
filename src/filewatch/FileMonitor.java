package filewatch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FileMonitor {

	private ExecutorService executor;

	private final WatchService watcher;

	public FileMonitor(Path dir, Consumer<File> file) throws IOException {
		executor = Executors.newSingleThreadExecutor();
		System.out.println(dir.normalize().toFile().getAbsolutePath());
		System.out.println(dir.toFile().exists());
		watcher = FileSystems.getDefault().newWatchService();
		try {
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		} catch (IOException x) {
			x.printStackTrace();
			return;
		}
		for (;;) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == OVERFLOW) {
					continue;
				}
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();
				Path child = dir.resolve(filename);
				if (!child.toFile().getName().endsWith(".lng")) {
					continue;
				}
				file.accept(child.toFile());
			}
			if (!key.reset()) {
				break;
			}
		}
	}

	public void cleanup() {
		try {
			watcher.close();
		} catch (IOException e) {
		}
		executor.shutdown();
	}

	private static Path targetExtFile(Path path, String ext) {
		String string = path.toFile().toString();
		return Paths.get(string.substring(0, string.lastIndexOf('.')) + ext);
	}

	protected static Path targetFile(Path srcPath, Path binPath, File file) {
		String[] srcComponents = srcPath.toFile().toString().split(File.separator);
		String[] binComponents = binPath.toFile().toString().split(File.separator);
		String[] fileComponents = file.toString().split(File.separator);
		String[] components = new String[binComponents.length + fileComponents.length - srcComponents.length];
		System.arraycopy(binComponents, 0, components, 0, binComponents.length);
		System.arraycopy(fileComponents, srcComponents.length, components, binComponents.length, fileComponents.length - srcComponents.length);
		return Paths.get(Arrays.stream(components).reduce((a, b) -> a + File.separator + b).get());
	}

}
