package io.zeebe.hazelcast.exporter;

import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class HazelcastRecordFilter implements Context.RecordFilter {

  private final List<RecordType> enabledRecordTypes;
  private final List<ValueType> enabledValueTypes;

  public HazelcastRecordFilter(ExporterConfiguration config) {

    final var enabledRecordTypeList = parseAsList(config.getEnabledRecordTypes());

    enabledRecordTypes =
        Arrays.stream(RecordType.values())
            .filter(
                recordType ->
                    enabledRecordTypeList.isEmpty()
                        || enabledRecordTypeList.contains(recordType.name()))
            .collect(Collectors.toList());

    final var enabledValueTypeList = parseAsList(config.getEnabledValueTypes());

    enabledValueTypes =
        Arrays.stream(ValueType.values())
            .filter(
                valueType ->
                    enabledValueTypeList.isEmpty()
                        || enabledValueTypeList.contains(valueType.name()))
            .collect(Collectors.toList());
  }

  private List<String> parseAsList(String list) {
    return Arrays.stream(list.split(","))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .map(String::toUpperCase)
        .collect(Collectors.toList());
  }

  @Override
  public boolean acceptType(RecordType recordType) {
    return enabledRecordTypes.contains(recordType);
  }

  @Override
  public boolean acceptValue(ValueType valueType) {
    return enabledValueTypes.contains(valueType);
  }
}
