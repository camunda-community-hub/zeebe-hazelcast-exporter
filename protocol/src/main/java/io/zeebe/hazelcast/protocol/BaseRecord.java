package io.zeebe.hazelcast.protocol;

public abstract class BaseRecord implements ZeebeEvent {

  private long key;
  private long timestamp;
  private String intent;

  @Override
public long getKey() {
    return key;
  }

  public void setKey(long key) {
    this.key = key;
  }

  @Override
public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

@Override
public String getIntent(){return intent;}

public void setIntent(String intent){this.intent = intent;}
}
