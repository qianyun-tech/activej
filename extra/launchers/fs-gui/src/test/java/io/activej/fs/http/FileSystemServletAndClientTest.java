package io.activej.fs.http;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufs;
import io.activej.common.exception.TruncatedDataException;
import io.activej.common.exception.UnexpectedDataException;
import io.activej.csp.consumer.ChannelConsumer;
import io.activej.csp.supplier.ChannelSupplier;
import io.activej.csp.supplier.ChannelSuppliers;
import io.activej.fs.FileMetadata;
import io.activej.fs.FileSystem;
import io.activej.fs.IFileSystem;
import io.activej.fs.exception.FileNotFoundException;
import io.activej.fs.exception.ForbiddenPathException;
import io.activej.http.AsyncServlet;
import io.activej.http.StubHttpClient;
import io.activej.reactor.Reactor;
import io.activej.test.ExpectedException;
import io.activej.test.rules.ByteBufRule;
import io.activej.test.rules.EventloopRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.activej.bytebuf.ByteBufStrings.wrapUtf8;
import static io.activej.promise.TestUtils.await;
import static io.activej.promise.TestUtils.awaitException;
import static io.activej.reactor.Reactor.getCurrentReactor;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public final class FileSystemServletAndClientTest {
	@ClassRule
	public static final EventloopRule eventloopRule = new EventloopRule();

	@ClassRule
	public static final ByteBufRule byteBufRule = new ByteBufRule();

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	private static final Set<String> initialFiles = Set.of("file", "file2", "directory/subdir/file3.txt", "directory/file.txt", "directory2/file2.txt");

	private Path storage;

	private IFileSystem fileSystem;

	@Before
	public void setUp() throws Exception {
		storage = tmpFolder.newFolder("storage").toPath();

		Reactor reactor = getCurrentReactor();
		FileSystem fileSystem = FileSystem.create(reactor, newSingleThreadExecutor(), storage);
		await(fileSystem.start());
		AsyncServlet servlet = FileSystemServlet.create(reactor, fileSystem);
		this.fileSystem = HttpClientFileSystem.create(reactor, "http://localhost", StubHttpClient.of(servlet));

		initializeDirs();
	}

	@Test
	public void list() {
		Map<String, FileMetadata> metadata = await(fileSystem.list("**"));
		assertEquals(initialFiles, metadata.keySet());
	}

	@Test
	public void upload() throws IOException {
		String content = "Test data";
		String fileName = "newDir/newFile";
		ByteBuf value = wrapUtf8(content);
		await(ChannelSuppliers.ofValue(value).streamTo(fileSystem.upload(fileName)));
		List<String> strings = Files.readAllLines(storage.resolve(fileName));

		assertEquals(1, strings.size());
		assertEquals(content, strings.get(0));
	}

	@Test
	public void uploadIncompleteFile() {
		String filename = "incomplete.txt";
		Path path = storage.resolve(filename);
		assertFalse(Files.exists(path));

		ExpectedException expectedException = new ExpectedException();
		ChannelConsumer<ByteBuf> consumer = await(fileSystem.upload(filename));

		Exception exception = awaitException(ChannelSuppliers.concat(
				ChannelSuppliers.ofValues(wrapUtf8("some"), wrapUtf8("test"), wrapUtf8("data")),
				ChannelSuppliers.ofException(expectedException))
			.streamTo(consumer));

		assertSame(expectedException, exception);

		assertFalse(Files.exists(path));
	}

	@Test
	public void uploadLessThanSpecified() {
		String filename = "incomplete.txt";
		Path path = storage.resolve(filename);
		assertFalse(Files.exists(path));

		ChannelConsumer<ByteBuf> consumer = await(fileSystem.upload(filename, 10));

		ByteBuf value = wrapUtf8("data");
		Exception exception = awaitException(ChannelSuppliers.ofValue(value).streamTo(consumer));

		assertThat(exception, instanceOf(TruncatedDataException.class));

		assertFalse(Files.exists(path));
	}

	@Test
	public void uploadMoreThanSpecified() {
		String filename = "incomplete.txt";
		Path path = storage.resolve(filename);
		assertFalse(Files.exists(path));

		ChannelConsumer<ByteBuf> consumer = await(fileSystem.upload(filename, 10));

		ByteBuf value = wrapUtf8("data data data data");
		Exception exception = awaitException(ChannelSuppliers.ofValue(value).streamTo(consumer));

		assertThat(exception, instanceOf(UnexpectedDataException.class));

		assertFalse(Files.exists(path));
	}

	@Test
	public void uploadIllegalPath() {
		ByteBuf value = wrapUtf8("test");
		Exception exception = awaitException(ChannelSuppliers.ofValue(value).streamTo(fileSystem.upload("../outside")));
		assertThat(exception, instanceOf(ForbiddenPathException.class));
	}

	@Test
	public void download() throws IOException {
		String fileName = "directory/subdir/file3.txt";
		ChannelSupplier<ByteBuf> supplier = await(fileSystem.download(fileName));
		ByteBuf result = await(supplier.toCollector(ByteBufs.collector()));
		byte[] expected = Files.readAllBytes(storage.resolve(fileName));

		assertArrayEquals(expected, result.asArray());
	}

	@Test
	public void downloadNonExistent() {
		Exception exception = awaitException(fileSystem.download("nonExistent"));
		assertThat(exception, instanceOf(FileNotFoundException.class));
	}

	private void initializeDirs() {
		try {
			for (String path : initialFiles) {
				Path file = this.storage.resolve(path);
				Files.createDirectories(file.getParent());
				Files.writeString(file, String.format("This is contents of file %s", file), CREATE, TRUNCATE_EXISTING);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
