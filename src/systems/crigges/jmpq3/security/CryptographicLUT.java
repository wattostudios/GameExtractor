
package systems.crigges.jmpq3.security;

/**
 * Cryptographic lookup tables used by MPQ for cryptographic operations such as
 * hashing and encryption. The tables translate byte values into seeded int
 * values.
 * <p>
 * MPQ uses 5 tables, each having a specific purpose.
 */
final class CryptographicLUT {

  /**
   * The number of cryptographic LUTs to generate. MPQ uses 5 tables.
   */
  private static final int TABLE_NUMBER = 5;

  /**
   * The number of values per cryptographic LUT. MPQ uses tables to translate
   * byte values into int values.
   */
  private static final int VALUE_NUMBER = 256;

  /**
   * Updates cryptographic table seed 1 cycle.
   * 
   * @param seed
   *            old seed.
   * @return new seed.
   */
  private static final int updateSeed(int seed) {
    return (seed * 125 + 3) % 0x2AAAAB;
  }

  /**
   * Master cryptographic translation tables.
   * <p>
   * Sub-tables of this are used by individual cryptographic LUT objects.
   */
  private static final int[][] CRYPTOGRAPHIC_TABLES = new int[TABLE_NUMBER][VALUE_NUMBER];
  static {
    // initial seed value
    int seed = 0x00100001;

    for (int value = 0; value < VALUE_NUMBER; value++) {
      for (int table = 0; table < TABLE_NUMBER; table++) {
        final short seed1 = (short) (seed = updateSeed(seed));
        final short seed2 = (short) (seed = updateSeed(seed));
        CRYPTOGRAPHIC_TABLES[table][value] = (int) seed1 << 16 | Short.toUnsignedInt(seed2);
      }
    }
  }

  /**
   * Table used to generate hashes for hashtable bucket array index.
   */
  public static final CryptographicLUT HASH_TABLE_OFFSET = new CryptographicLUT(0);

  /**
   * Table used to generate hashes for part 1 of hashtable keys.
   */
  public static final CryptographicLUT HASH_TABLE_KEY1 = new CryptographicLUT(1);

  /**
   * Table used to generate hashes for part 2 of hashtable keys.
   */
  public static final CryptographicLUT HASH_TABLE_KEY2 = new CryptographicLUT(2);

  /**
   * Table used to generate hashes for MPQ encryption keys.
   */
  public static final CryptographicLUT HASH_ENCRYPTION_KEY = new CryptographicLUT(3);

  /**
   * Table used to encrypt data.
   */
  public static final CryptographicLUT ENCRYPTION = new CryptographicLUT(4);

  /**
   * The cryptographic LUT to use for lookup operators.
   */
  private final int[] cryptographicLUT;

  /**
   * Generates a cryptographic LUT with the specified table index.
   * 
   * @param table
   *            the table index to use.
   */
  private CryptographicLUT(int table) {
    cryptographicLUT = CRYPTOGRAPHIC_TABLES[table];
  }

  /**
   * Lookup value using this cryptographic LUT.
   * 
   * @param value
   *            value being looked up.
   * @return cryptographic int from the LUT.
   */
  public int lookup(byte value) {
    return cryptographicLUT[Byte.toUnsignedInt(value)];
  }
}