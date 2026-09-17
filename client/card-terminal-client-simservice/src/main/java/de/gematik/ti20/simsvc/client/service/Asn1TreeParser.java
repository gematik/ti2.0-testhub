/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Recursive ASN.1 DER parser that returns a traversable object tree. */
public final class Asn1TreeParser {

  public Asn1Node parse(byte[] encoded) {
    if (encoded == null) {
      throw new IllegalArgumentException("encoded must not be null");
    }
    ParseResult result = parseNodes(encoded, 0, encoded.length);
    if (result.nextOffset != encoded.length) {
      throw new IllegalArgumentException("Trailing bytes after ASN.1 parsing");
    }
    List<Asn1Node> normalized = unwrapSingleEncapsulatedOctetString(result.nodes);
    return new Asn1Node(-1, 0, true, encoded, normalized);
  }

  public static List<String> collectLeafValuesAsString(Asn1Node root) {
    if (root == null) {
      return List.of();
    }
    List<String> leaves = new ArrayList<>();
    collectLeafValuesAsStringRecursive(root, leaves);
    return leaves;
  }

  private static void collectLeafValuesAsStringRecursive(Asn1Node node, List<String> leaves) {
    if (node.children.isEmpty()) {
      if (node.value.length > 0) {
        leaves.add(serializeLeafNode(node));
      }
      return;
    }
    for (Asn1Node child : node.children) {
      collectLeafValuesAsStringRecursive(child, leaves);
    }
  }

  private static String serializeLeafNode(Asn1Node node) {
    int tag = node.tag & 0xFF;
    byte[] value = node.value;
    switch (tag) {
      case 0x01:
        return value.length > 0 && value[0] != 0 ? "true" : "false";
      case 0x02:
        return new java.math.BigInteger(value).toString();
      case 0x03, 0x04:
        return toHex(value);
      case 0x05:
        return "NULL";
      case 0x06:
        return decodeOid(value);
      case 0x0C, 0x13, 0x16, 0x1A:
        return new String(value, StandardCharsets.UTF_8);
      case 0x17, 0x18:
        return new String(value, StandardCharsets.US_ASCII);
      case 0x1E:
        return new String(value, java.nio.charset.StandardCharsets.UTF_16BE);
      default:
        return toHex(value);
    }
  }

  private static String decodeOid(byte[] value) {
    if (value.length == 0) {
      return "";
    }
    StringBuilder oid = new StringBuilder();
    int first = value[0] & 0xFF;
    int firstArc = first / 40;
    int secondArc = first % 40;
    oid.append(firstArc).append('.').append(secondArc);

    long current = 0;
    for (int i = 1; i < value.length; i++) {
      int b = value[i] & 0xFF;
      current = (current << 7) | (b & 0x7F);
      if ((b & 0x80) == 0) {
        oid.append('.').append(current);
        current = 0;
      }
    }
    if (current != 0) {
      return toHex(value);
    }
    return oid.toString();
  }

  private static String toHex(byte[] data) {
    StringBuilder sb = new StringBuilder(data.length * 2);
    for (byte b : data) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString().toUpperCase();
  }

  private ParseResult parseNodes(byte[] data, int offset, int limit) {
    List<Asn1Node> nodes = new ArrayList<>();
    int cursor = offset;
    while (cursor < limit) {
      ParsedNode parsedNode = parseNode(data, cursor, limit);
      nodes.add(parsedNode.node);
      cursor = parsedNode.nextOffset;
    }
    return new ParseResult(nodes, cursor);
  }

  private ParsedNode parseNode(byte[] data, int offset, int limit) {
    int tag = readTag(data, offset);
    int valueOffset = offset + 1;
    int valueLength = readValueLength(data, valueOffset, limit);
    int contentOffset = valueOffset + countLengthBytes(data, valueOffset);
    byte[] value =
        copyValue(data, contentOffset, valueLength, remainingLength(contentOffset, limit));
    return new ParsedNode(
        new Asn1Node(tag, offset, isConstructed(tag), value, parseChildren(tag, value)),
        contentOffset + valueLength);
  }

  private int remainingLength(int offset, int limit) {
    return limit - offset;
  }

  private int readTag(byte[] data, int offset) {
    return data[offset] & 0xFF;
  }

  private int readValueLength(byte[] data, int lengthOffset, int limit) {
    ensureLengthBytePresent(lengthOffset, limit);
    int lengthByte = data[lengthOffset] & 0xFF;
    if ((lengthByte & 0x80) == 0) {
      return lengthByte;
    }
    return readMultiByteLength(data, lengthOffset, limit, lengthByte);
  }

  private void ensureLengthBytePresent(int lengthOffset, int limit) {
    if (lengthOffset >= limit) {
      throw new IllegalArgumentException("Invalid ASN.1: missing length");
    }
  }

  private int readMultiByteLength(byte[] data, int lengthOffset, int limit, int lengthByte) {
    int numLengthBytes = lengthByte & 0x7F;
    if (numLengthBytes == 0) {
      throw new IllegalArgumentException("Indefinite length is not supported for DER");
    }
    if (numLengthBytes > 4) {
      throw new IllegalArgumentException("Unsupported ASN.1 length encoding");
    }
    int firstLengthByteOffset = lengthOffset + 1;
    if (firstLengthByteOffset + numLengthBytes > limit) {
      throw new IllegalArgumentException("Invalid ASN.1: truncated length bytes");
    }
    int length = 0;
    for (int i = 0; i < numLengthBytes; i++) {
      length = (length << 8) | (data[firstLengthByteOffset + i] & 0xFF);
    }
    return length;
  }

  private int countLengthBytes(byte[] data, int lengthOffset) {
    int lengthByte = data[lengthOffset] & 0xFF;
    return (lengthByte & 0x80) == 0 ? 1 : 1 + (lengthByte & 0x7F);
  }

  private byte[] copyValue(byte[] data, int valueOffset, int valueLength, int availableLength) {
    if (valueLength > availableLength) {
      throw new IllegalArgumentException("Invalid ASN.1: value exceeds bounds");
    }
    byte[] value = new byte[valueLength];
    System.arraycopy(data, valueOffset, value, 0, valueLength);
    return value;
  }

  private boolean isConstructed(int tag) {
    return (tag & 0x20) != 0;
  }

  private List<Asn1Node> parseChildren(int tag, byte[] value) {
    if (isConstructed(tag)) {
      return parseConstructedChildren(value);
    }
    if (isUniversalOctetString(tag)) {
      return parseOctetStringChildren(value);
    }
    return Collections.emptyList();
  }

  private List<Asn1Node> parseConstructedChildren(byte[] value) {
    ParseResult childResult = parseNodes(value, 0, value.length);
    if (childResult.nextOffset != value.length) {
      throw new IllegalArgumentException("Invalid ASN.1: unparsed child bytes");
    }
    return childResult.nodes;
  }

  private List<Asn1Node> parseOctetStringChildren(byte[] value) {
    List<Asn1Node> octetChildren = tryParseEmbeddedAsn1(value);
    return octetChildren.isEmpty() ? Collections.emptyList() : octetChildren;
  }

  private List<Asn1Node> unwrapSingleEncapsulatedOctetString(List<Asn1Node> nodes) {
    List<Asn1Node> current = nodes;
    while (current.size() == 1) {
      Asn1Node single = current.get(0);
      if (!isUniversalOctetString(single.tag)) {
        break;
      }
      if (single.children.isEmpty()) {
        break;
      }
      current = single.children;
    }
    return current;
  }

  private boolean isUniversalOctetString(int tag) {
    return (tag & 0xFF) == 0x04;
  }

  private List<Asn1Node> tryParseEmbeddedAsn1(byte[] candidate) {
    try {
      ParseResult childResult = parseNodes(candidate, 0, candidate.length);
      if (childResult.nextOffset == candidate.length && !childResult.nodes.isEmpty()) {
        return childResult.nodes;
      }
    } catch (IllegalArgumentException ignored) {
      // Not ASN.1 payload -> keep OCTET STRING as primitive node.
    }
    return Collections.emptyList();
  }

  private static final class ParseResult {
    private final List<Asn1Node> nodes;
    private final int nextOffset;

    private ParseResult(List<Asn1Node> nodes, int nextOffset) {
      this.nodes = nodes;
      this.nextOffset = nextOffset;
    }
  }

  private static final class ParsedNode {
    private final Asn1Node node;
    private final int nextOffset;

    private ParsedNode(Asn1Node node, int nextOffset) {
      this.node = node;
      this.nextOffset = nextOffset;
    }
  }

  record Asn1Node(int tag, int offset, boolean constructed, byte[] value, List<Asn1Node> children) {

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Asn1Node other)) {
        return false;
      }
      return tag == other.tag
          && offset == other.offset
          && constructed == other.constructed
          && Arrays.equals(value, other.value)
          && Objects.equals(children, other.children);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tag, offset, constructed, Arrays.hashCode(value), children);
    }

    @Override
    public String toString() {
      return "Asn1Node{"
          + "tag="
          + tag
          + ", offset="
          + offset
          + ", constructed="
          + constructed
          + ", value="
          + Arrays.toString(value)
          + ", children="
          + children
          + '}';
    }
  }
}
