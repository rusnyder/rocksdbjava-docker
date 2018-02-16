import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;
import org.rocksdb.StringAppendOperator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Sampling of tests copied in from the RocksDB project just to check
 * that the installation of RocksDB in the docker image is valid.
 * <p>
 * For more information, see the RocksDB project:
 * https://github.com/facebook/rocksdb/blob/master/java/src/test/java/org/rocksdb/RocksDBTest.java
 * </p>
 */
public class RocksDBTest {

    private static final List<CompressionType> SUPPORTED_COMPRESSION = ImmutableList.of(
            CompressionType.NO_COMPRESSION,
            CompressionType.SNAPPY_COMPRESSION,
            CompressionType.ZLIB_COMPRESSION,
            CompressionType.ZSTD_COMPRESSION,
            CompressionType.DISABLE_COMPRESSION_OPTION
    );

    @Rule
    public TemporaryFolder dbFolder = new TemporaryFolder();

    @Rule
    public ExpectedException errors = ExpectedException.none();

    @Test
    public void open() throws RocksDBException {
        try (final RocksDB db =
                     RocksDB.open(dbFolder.getRoot().getAbsolutePath())) {
            assertThat(db).isNotNull();
        }
    }

    @Test
    public void open_opt() throws RocksDBException {
        try (final Options opt = new Options().setCreateIfMissing(true);
             final RocksDB db = RocksDB.open(opt,
                     dbFolder.getRoot().getAbsolutePath())) {
            assertThat(db).isNotNull();
        }
    }

    @Test
    public void openWhenOpen() throws RocksDBException {
        final String dbPath = dbFolder.getRoot().getAbsolutePath();

        try (final RocksDB db1 = RocksDB.open(dbPath)) {
            try (final RocksDB db2 = RocksDB.open(dbPath)) {
                fail("Should have thrown an exception when opening the same db twice");
            } catch (final RocksDBException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.IOError);
                assertThat(e.getStatus().getSubCode()).isEqualTo(Status.SubCode.None);
                assertThat(e.getStatus().getState()).contains("lock ");
            }
        }
    }

    @Test
    public void put() throws RocksDBException {
        try (final RocksDB db = RocksDB.open(dbFolder.getRoot().getAbsolutePath());
             final WriteOptions opt = new WriteOptions()) {
            db.put("key1".getBytes(), "value".getBytes());
            db.put(opt, "key2".getBytes(), "12345678".getBytes());
            assertThat(db.get("key1".getBytes())).isEqualTo(
                    "value".getBytes());
            assertThat(db.get("key2".getBytes())).isEqualTo(
                    "12345678".getBytes());
        }
    }

    @Test
    public void write() throws RocksDBException {
        try (final StringAppendOperator stringAppendOperator = new StringAppendOperator();
             final Options options = new Options()
                     .setMergeOperator(stringAppendOperator)
                     .setCreateIfMissing(true);
             final RocksDB db = RocksDB.open(options,
                     dbFolder.getRoot().getAbsolutePath());
             final WriteOptions opts = new WriteOptions()) {

            try (final WriteBatch wb1 = new WriteBatch()) {
                wb1.put("key1".getBytes(), "aa".getBytes());
                wb1.merge("key1".getBytes(), "bb".getBytes());

                try (final WriteBatch wb2 = new WriteBatch()) {
                    wb2.put("key2".getBytes(), "xx".getBytes());
                    wb2.merge("key2".getBytes(), "yy".getBytes());
                    db.write(opts, wb1);
                    db.write(opts, wb2);
                }
            }

            assertThat(db.get("key1".getBytes())).isEqualTo(
                    "aa,bb".getBytes());
            assertThat(db.get("key2".getBytes())).isEqualTo(
                    "xx,yy".getBytes());
        }
    }

    @Test
    public void multiGet() throws RocksDBException, InterruptedException {
        try (final RocksDB db = RocksDB.open(dbFolder.getRoot().getAbsolutePath());
             final ReadOptions rOpt = new ReadOptions()) {
            db.put("key1".getBytes(), "value".getBytes());
            db.put("key2".getBytes(), "12345678".getBytes());
            List<byte[]> lookupKeys = new ArrayList<>();
            lookupKeys.add("key1".getBytes());
            lookupKeys.add("key2".getBytes());
            Map<byte[], byte[]> results = db.multiGet(lookupKeys);
            assertThat(results).isNotNull();
            assertThat(results.values()).isNotNull();
            assertThat(results.values()).
                    contains("value".getBytes(), "12345678".getBytes());
            // test same method with ReadOptions
            results = db.multiGet(rOpt, lookupKeys);
            assertThat(results).isNotNull();
            assertThat(results.values()).isNotNull();
            assertThat(results.values()).
                    contains("value".getBytes(), "12345678".getBytes());

            // remove existing key
            lookupKeys.remove("key2".getBytes());
            // add non existing key
            lookupKeys.add("key3".getBytes());
            results = db.multiGet(lookupKeys);
            assertThat(results).isNotNull();
            assertThat(results.values()).isNotNull();
            assertThat(results.values()).
                    contains("value".getBytes());
            // test same call with readOptions
            results = db.multiGet(rOpt, lookupKeys);
            assertThat(results).isNotNull();
            assertThat(results.values()).isNotNull();
            assertThat(results.values()).
                    contains("value".getBytes());
        }
    }

    @Test
    public void delete() throws RocksDBException {
        try (final RocksDB db = RocksDB.open(dbFolder.getRoot().getAbsolutePath());
             final WriteOptions wOpt = new WriteOptions()) {
            db.put("key1".getBytes(), "value".getBytes());
            db.put("key2".getBytes(), "12345678".getBytes());
            assertThat(db.get("key1".getBytes())).isEqualTo(
                    "value".getBytes());
            assertThat(db.get("key2".getBytes())).isEqualTo(
                    "12345678".getBytes());
            db.delete("key1".getBytes());
            db.delete(wOpt, "key2".getBytes());
            assertThat(db.get("key1".getBytes())).isNull();
            assertThat(db.get("key2".getBytes())).isNull();
        }
    }

    @Test(expected = RocksDBException.class)
    public void destroyDBFailIfOpen() throws RocksDBException {
        try (final Options options = new Options().setCreateIfMissing(true)) {
            String dbPath = dbFolder.getRoot().getAbsolutePath();
            try (final RocksDB db = RocksDB.open(options, dbPath)) {
                // Fails as the db is open and locked.
                RocksDB.destroyDB(dbPath, options);
            }
        }
    }

    @Test
    public void testAvailableCompressionTypes() throws Exception {
        // Test
        for (CompressionType compressionType : CompressionType.values()) {
            if (!SUPPORTED_COMPRESSION.contains(compressionType)) {
                errors.expect(RocksDBException.class);
            }
            try (final Options opt = new Options()
                    .setCompressionType(compressionType)
                    .setCreateIfMissing(true);
                 final RocksDB db = RocksDB.open(
                         opt, dbFolder.getRoot().getAbsolutePath())) {
                assertThat(db).isNotNull();
                assertThat(opt.compressionType()).isEqualTo(compressionType);
            }
        }
    }
}
