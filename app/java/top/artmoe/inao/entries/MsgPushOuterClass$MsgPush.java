package top.artmoe.inao.entries;

public final class MsgPushOuterClass$MsgPush extends com.google.protobuf.GeneratedMessageLite<MsgPushOuterClass$MsgPush, MsgPushOuterClass$MsgPush.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
    private static final top.artmoe.inao.entries.MsgPushOuterClass$MsgPush DEFAULT_INSTANCE;
    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.MsgPushOuterClass$MsgPush> PARSER = null;
    public static final int QQMESSAGE_FIELD_NUMBER = 1;
    private int bitField0_;
    private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qqMessage_;

    private MsgPushOuterClass$MsgPush() {
    }

    public boolean hasQqMessage() {
        return (this.bitField0_ & 1) != 0;
    }

    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage getQqMessage() {
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage = this.qqMessage_;
        return qQMessageOuterClass$QQMessage == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.getDefaultInstance() : qQMessageOuterClass$QQMessage;
    }

    public void setQqMessage(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
        qQMessageOuterClass$QQMessage.getClass();
        this.qqMessage_ = qQMessageOuterClass$QQMessage;
        this.bitField0_ |= 1;
    }

    private void mergeQqMessage(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
        qQMessageOuterClass$QQMessage.getClass();
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage2 = this.qqMessage_;
        if (qQMessageOuterClass$QQMessage2 != null && qQMessageOuterClass$QQMessage2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.getDefaultInstance()) {
            this.qqMessage_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.newBuilder(this.qqMessage_).mergeFrom((com.google.protobuf.GeneratedMessageLite) qQMessageOuterClass$QQMessage)).buildPartial();
        } else {
            this.qqMessage_ = qQMessageOuterClass$QQMessage;
        }
        this.bitField0_ |= 1;
    }

    private void clearQqMessage() {
        this.qqMessage_ = null;
        this.bitField0_ &= -2;
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder newBuilder() {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder) DEFAULT_INSTANCE.createBuilder();
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder newBuilder(top.artmoe.inao.entries.MsgPushOuterClass$MsgPush msgPushOuterClass$MsgPush) {
        return (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder) DEFAULT_INSTANCE.createBuilder(msgPushOuterClass$MsgPush);
    }

    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MsgPushOuterClass$MsgPush, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        public Builder() {
            super(top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.DEFAULT_INSTANCE);
        }

        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage getQqMessage() {
            return ((top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) this.instance).getQqMessage();
        }

        public top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder setQqMessage(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
            copyOnWrite();
            ((top.artmoe.inao.entries.MsgPushOuterClass$MsgPush) this.instance).setQqMessage(qQMessageOuterClass$QQMessage);
            return this;
        }
    }

    @Override
    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
        int i = top.artmoe.inao.entries.MsgPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
        switch (i) {
            case 1:
                return new top.artmoe.inao.entries.MsgPushOuterClass$MsgPush();
            case 2:
                return new top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.Builder();
            case 3:
                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0000\u0000\u0000\u0001ဉ\u0000", new java.lang.Object[]{"bitField0_", "qqMessage_"});
            case 4:
                return DEFAULT_INSTANCE;
            case 5:
                com.google.protobuf.Parser<top.artmoe.inao.entries.MsgPushOuterClass$MsgPush> parser = PARSER;
                if (parser == null) {
                    synchronized (top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.class) {
                        try {
                            parser = PARSER;
                            if (parser == null) {
                                parser = new com.google.protobuf.GeneratedMessageLite.DefaultInstanceBasedParser<>(DEFAULT_INSTANCE);
                                PARSER = parser;
                            }
                        } finally {
                        }
                    }
                }
                return parser;
            case 6:
                return (byte) 1;
            case 7:
                return null;
            default:
                throw new java.lang.UnsupportedOperationException();
        }
    }

    static {
        top.artmoe.inao.entries.MsgPushOuterClass$MsgPush msgPushOuterClass$MsgPush = new top.artmoe.inao.entries.MsgPushOuterClass$MsgPush();
        DEFAULT_INSTANCE = msgPushOuterClass$MsgPush;
        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.MsgPushOuterClass$MsgPush.class, msgPushOuterClass$MsgPush);
    }

    public static top.artmoe.inao.entries.MsgPushOuterClass$MsgPush getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<top.artmoe.inao.entries.MsgPushOuterClass$MsgPush> parser() {
        return DEFAULT_INSTANCE.getParserForType();
    }
}
