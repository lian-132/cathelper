package top.artmoe.inao.entries;

public final class InfoSyncPushOuterClass$InfoSyncPush extends com.google.protobuf.GeneratedMessageLite<InfoSyncPushOuterClass$InfoSyncPush, InfoSyncPushOuterClass$InfoSyncPush.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
    private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush DEFAULT_INSTANCE;
    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush> PARSER = null;
    public static final int PUSHID_FIELD_NUMBER = 4;
    public static final int SYNCCONTENT_FIELD_NUMBER = 7;
    public static final int SYNCRECALLCONTENT_FIELD_NUMBER = 8;
    public static final int TYPE_FIELD_NUMBER = 3;
    private int bitField0_;
    private int pushId_;
    private top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent_;
    private top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallContent_;
    private int type_;

    private InfoSyncPushOuterClass$InfoSyncPush() {
    }

    public static final class SyncContent extends com.google.protobuf.GeneratedMessageLite<SyncContent, SyncContent.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent DEFAULT_INSTANCE;
        public static final int GROUPSYNCCONTENT_FIELD_NUMBER = 3;
        private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent> PARSER;
        private com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> groupSyncContent_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();

        public interface GroupSyncContentOrBuilder extends com.google.protobuf.MessageLiteOrBuilder {
        }

        private SyncContent() {
        }

        public static final class GroupSyncContent extends com.google.protobuf.GeneratedMessageLite<GroupSyncContent, GroupSyncContent.Builder> implements top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContentOrBuilder {
            private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent DEFAULT_INSTANCE;
            public static final int ENDSEQ_FIELD_NUMBER = 5;
            public static final int GROUPPEERID_FIELD_NUMBER = 3;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> PARSER = null;
            public static final int QQMESSAGE_FIELD_NUMBER = 6;
            public static final int STARTSEQ_FIELD_NUMBER = 4;
            private int endSeq_;
            private long groupPeerId_;
            private com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> qqMessage_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
            private int startSeq_;

            private GroupSyncContent() {
            }

            public long getGroupPeerId() {
                return this.groupPeerId_;
            }

            private void setGroupPeerId(long j) {
                this.groupPeerId_ = j;
            }

            private void clearGroupPeerId() {
                this.groupPeerId_ = 0L;
            }

            public int getStartSeq() {
                return this.startSeq_;
            }

            private void setStartSeq(int i) {
                this.startSeq_ = i;
            }

            private void clearStartSeq() {
                this.startSeq_ = 0;
            }

            public int getEndSeq() {
                return this.endSeq_;
            }

            private void setEndSeq(int i) {
                this.endSeq_ = i;
            }

            private void clearEndSeq() {
                this.endSeq_ = 0;
            }

            public java.util.List<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> getQqMessageList() {
                return this.qqMessage_;
            }

            public java.util.List<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder> getQqMessageOrBuilderList() {
                return this.qqMessage_;
            }

            public int getQqMessageCount() {
                return this.qqMessage_.size();
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage getQqMessage(int i) {
                return this.qqMessage_.get(i);
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder getQqMessageOrBuilder(int i) {
                return this.qqMessage_.get(i);
            }

            private void ensureQqMessageIsMutable() {
                com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> protobufList = this.qqMessage_;
                if (protobufList.isModifiable()) {
                    return;
                }
                this.qqMessage_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(protobufList);
            }

            private void setQqMessage(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureQqMessageIsMutable();
                this.qqMessage_.set(i, qQMessageOuterClass$QQMessage);
            }

            private void addQqMessage(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureQqMessageIsMutable();
                this.qqMessage_.add(qQMessageOuterClass$QQMessage);
            }

            private void addQqMessage(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureQqMessageIsMutable();
                this.qqMessage_.add(i, qQMessageOuterClass$QQMessage);
            }

            private void addAllQqMessage(java.lang.Iterable<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> iterable) {
                ensureQqMessageIsMutable();
                com.google.protobuf.AbstractMessageLite.addAll(iterable, this.qqMessage_);
            }

            private void clearQqMessage() {
                this.qqMessage_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
            }

            private void removeQqMessage(int i) {
                ensureQqMessageIsMutable();
                this.qqMessage_.remove(i);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.Builder newBuilder() {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent groupSyncContent) {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.Builder) DEFAULT_INSTANCE.createBuilder(groupSyncContent);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<GroupSyncContent, Builder> implements top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContentOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.DEFAULT_INSTANCE);
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent();
                    case 2:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0004\u0000\u0000\u0003\u0006\u0004\u0000\u0001\u0000\u0003\u0002\u0004\u0004\u0005\u0004\u0006\u001b", new java.lang.Object[]{"groupPeerId_", "startSeq_", "endSeq_", "qqMessage_", top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.class});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.class) {
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
                top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent groupSyncContent = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent();
                DEFAULT_INSTANCE = groupSyncContent;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.class, groupSyncContent);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public java.util.List<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> getGroupSyncContentList() {
            return this.groupSyncContent_;
        }

        public java.util.List<? extends top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContentOrBuilder> getGroupSyncContentOrBuilderList() {
            return this.groupSyncContent_;
        }

        public int getGroupSyncContentCount() {
            return this.groupSyncContent_.size();
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent getGroupSyncContent(int i) {
            return this.groupSyncContent_.get(i);
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContentOrBuilder getGroupSyncContentOrBuilder(int i) {
            return this.groupSyncContent_.get(i);
        }

        private void ensureGroupSyncContentIsMutable() {
            com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> protobufList = this.groupSyncContent_;
            if (protobufList.isModifiable()) {
                return;
            }
            this.groupSyncContent_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(protobufList);
        }

        private void setGroupSyncContent(int i, top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent groupSyncContent) {
            groupSyncContent.getClass();
            ensureGroupSyncContentIsMutable();
            this.groupSyncContent_.set(i, groupSyncContent);
        }

        private void addGroupSyncContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent groupSyncContent) {
            groupSyncContent.getClass();
            ensureGroupSyncContentIsMutable();
            this.groupSyncContent_.add(groupSyncContent);
        }

        private void addGroupSyncContent(int i, top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent groupSyncContent) {
            groupSyncContent.getClass();
            ensureGroupSyncContentIsMutable();
            this.groupSyncContent_.add(i, groupSyncContent);
        }

        private void addAllGroupSyncContent(java.lang.Iterable<? extends top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent> iterable) {
            ensureGroupSyncContentIsMutable();
            com.google.protobuf.AbstractMessageLite.addAll(iterable, this.groupSyncContent_);
        }

        private void clearGroupSyncContent() {
            this.groupSyncContent_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
        }

        private void removeGroupSyncContent(int i) {
            ensureGroupSyncContentIsMutable();
            this.groupSyncContent_.remove(i);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder newBuilder() {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder) DEFAULT_INSTANCE.createBuilder();
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent) {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder) DEFAULT_INSTANCE.createBuilder(syncContent);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<SyncContent, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            public Builder() {
                super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.DEFAULT_INSTANCE);
            }
        }

        @Override
        public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
            int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
            switch (i) {
                case 1:
                    return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent();
                case 2:
                    return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder();
                case 3:
                    return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0000\u0003\u0003\u0001\u0000\u0001\u0000\u0003\u001b", new java.lang.Object[]{"groupSyncContent_", top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.GroupSyncContent.class});
                case 4:
                    return DEFAULT_INSTANCE;
                case 5:
                    com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent> parser = PARSER;
                    if (parser == null) {
                        synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.class) {
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
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent();
            DEFAULT_INSTANCE = syncContent;
            com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.class, syncContent);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class SyncRecallOperateInfo extends com.google.protobuf.GeneratedMessageLite<SyncRecallOperateInfo, SyncRecallOperateInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo DEFAULT_INSTANCE;
        private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo> PARSER = null;
        public static final int SUBHEAD_FIELD_NUMBER = 5;
        public static final int SYNCINFOBODY_FIELD_NUMBER = 4;
        public static final int SYNCINFOHEAD_FIELD_NUMBER = 3;
        private int bitField0_;
        private top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead subHead_;
        private com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> syncInfoBody_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
        private top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead_;

        public interface SyncInfoBodyOrBuilder extends com.google.protobuf.MessageLiteOrBuilder {
        }

        private SyncRecallOperateInfo() {
        }

        public static final class SyncInfoHead extends com.google.protobuf.GeneratedMessageLite<SyncInfoHead, SyncInfoHead.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead DEFAULT_INSTANCE;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead> PARSER = null;
            public static final int SYNCTIME_FIELD_NUMBER = 1;
            private long syncTime_;

            private SyncInfoHead() {
            }

            public long getSyncTime() {
                return this.syncTime_;
            }

            private void setSyncTime(long j) {
                this.syncTime_ = j;
            }

            private void clearSyncTime() {
                this.syncTime_ = 0L;
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder newBuilder() {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead) {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder) DEFAULT_INSTANCE.createBuilder(syncInfoHead);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<SyncInfoHead, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.DEFAULT_INSTANCE);
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead();
                    case 2:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001\u0002", new java.lang.Object[]{"syncTime_"});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.class) {
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
                top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead();
                DEFAULT_INSTANCE = syncInfoHead;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.class, syncInfoHead);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public static final class SyncInfoBody extends com.google.protobuf.GeneratedMessageLite<SyncInfoBody, SyncInfoBody.Builder> implements top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBodyOrBuilder {
            private static final top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody DEFAULT_INSTANCE;
            public static final int EVENTTIME_FIELD_NUMBER = 5;
            public static final int MSG_FIELD_NUMBER = 8;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> PARSER = null;
            public static final int SENDERPEERID_FIELD_NUMBER = 1;
            public static final int SENDERUID_FIELD_NUMBER = 2;
            private long eventTime_;
            private long senderPeerId_;
            private java.lang.String senderUid_ = "";
            private com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> msg_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();

            private SyncInfoBody() {
            }

            public long getSenderPeerId() {
                return this.senderPeerId_;
            }

            private void setSenderPeerId(long j) {
                this.senderPeerId_ = j;
            }

            private void clearSenderPeerId() {
                this.senderPeerId_ = 0L;
            }

            public java.lang.String getSenderUid() {
                return this.senderUid_;
            }

            public com.google.protobuf.ByteString getSenderUidBytes() {
                return com.google.protobuf.ByteString.copyFromUtf8(this.senderUid_);
            }

            private void setSenderUid(java.lang.String str) {
                str.getClass();
                this.senderUid_ = str;
            }

            private void clearSenderUid() {
                this.senderUid_ = getDefaultInstance().getSenderUid();
            }

            private void setSenderUidBytes(com.google.protobuf.ByteString byteString) {
                com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                this.senderUid_ = byteString.toStringUtf8();
            }

            public long getEventTime() {
                return this.eventTime_;
            }

            private void setEventTime(long j) {
                this.eventTime_ = j;
            }

            private void clearEventTime() {
                this.eventTime_ = 0L;
            }

            public java.util.List<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> getMsgList() {
                return this.msg_;
            }

            public java.util.List<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder> getMsgOrBuilderList() {
                return this.msg_;
            }

            public int getMsgCount() {
                return this.msg_.size();
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage getMsg(int i) {
                return this.msg_.get(i);
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder getMsgOrBuilder(int i) {
                return this.msg_.get(i);
            }

            private void ensureMsgIsMutable() {
                com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> protobufList = this.msg_;
                if (protobufList.isModifiable()) {
                    return;
                }
                this.msg_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(protobufList);
            }

            private void setMsg(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureMsgIsMutable();
                this.msg_.set(i, qQMessageOuterClass$QQMessage);
            }

            private void addMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureMsgIsMutable();
                this.msg_.add(qQMessageOuterClass$QQMessage);
            }

            private void addMsg(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
                qQMessageOuterClass$QQMessage.getClass();
                ensureMsgIsMutable();
                this.msg_.add(i, qQMessageOuterClass$QQMessage);
            }

            private void addAllMsg(java.lang.Iterable<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> iterable) {
                ensureMsgIsMutable();
                com.google.protobuf.AbstractMessageLite.addAll(iterable, this.msg_);
            }

            public void clearMsg() {
                this.msg_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
            }

            private void removeMsg(int i) {
                ensureMsgIsMutable();
                this.msg_.remove(i);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder newBuilder() {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody) {
                return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder) DEFAULT_INSTANCE.createBuilder(syncInfoBody);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<SyncInfoBody, Builder> implements top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBodyOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.DEFAULT_INSTANCE);
                }

                public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder clearMsg() {
                    copyOnWrite();
                    ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody) this.instance).clearMsg();
                    return this;
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody();
                    case 2:
                        return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0004\u0000\u0000\u0001\b\u0004\u0000\u0001\u0000\u0001\u0002\u0002Ȉ\u0005\u0002\b\u001b", new java.lang.Object[]{"senderPeerId_", "senderUid_", "eventTime_", "msg_", top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.class});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.class) {
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
                top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody();
                DEFAULT_INSTANCE = syncInfoBody;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.class, syncInfoBody);
            }

            public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public boolean hasSyncInfoHead() {
            return (this.bitField0_ & 1) != 0;
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead getSyncInfoHead() {
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead = this.syncInfoHead_;
            return syncInfoHead == null ? top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.getDefaultInstance() : syncInfoHead;
        }

        private void setSyncInfoHead(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead) {
            syncInfoHead.getClass();
            this.syncInfoHead_ = syncInfoHead;
            this.bitField0_ |= 1;
        }

        private void mergeSyncInfoHead(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead) {
            syncInfoHead.getClass();
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead2 = this.syncInfoHead_;
            if (syncInfoHead2 != null && syncInfoHead2 != top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.getDefaultInstance()) {
                this.syncInfoHead_ = (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder) top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.newBuilder(this.syncInfoHead_).mergeFrom((com.google.protobuf.GeneratedMessageLite) syncInfoHead)).buildPartial();
            } else {
                this.syncInfoHead_ = syncInfoHead;
            }
            this.bitField0_ |= 1;
        }

        private void clearSyncInfoHead() {
            this.syncInfoHead_ = null;
            this.bitField0_ &= -2;
        }

        public java.util.List<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> getSyncInfoBodyList() {
            return this.syncInfoBody_;
        }

        public java.util.List<? extends top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBodyOrBuilder> getSyncInfoBodyOrBuilderList() {
            return this.syncInfoBody_;
        }

        public int getSyncInfoBodyCount() {
            return this.syncInfoBody_.size();
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody getSyncInfoBody(int i) {
            return this.syncInfoBody_.get(i);
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBodyOrBuilder getSyncInfoBodyOrBuilder(int i) {
            return this.syncInfoBody_.get(i);
        }

        private void ensureSyncInfoBodyIsMutable() {
            com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> protobufList = this.syncInfoBody_;
            if (protobufList.isModifiable()) {
                return;
            }
            this.syncInfoBody_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(protobufList);
        }

        public void setSyncInfoBody(int i, top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody) {
            syncInfoBody.getClass();
            ensureSyncInfoBodyIsMutable();
            this.syncInfoBody_.set(i, syncInfoBody);
        }

        private void addSyncInfoBody(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody) {
            syncInfoBody.getClass();
            ensureSyncInfoBodyIsMutable();
            this.syncInfoBody_.add(syncInfoBody);
        }

        private void addSyncInfoBody(int i, top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody) {
            syncInfoBody.getClass();
            ensureSyncInfoBodyIsMutable();
            this.syncInfoBody_.add(i, syncInfoBody);
        }

        private void addAllSyncInfoBody(java.lang.Iterable<? extends top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody> iterable) {
            ensureSyncInfoBodyIsMutable();
            com.google.protobuf.AbstractMessageLite.addAll(iterable, this.syncInfoBody_);
        }

        private void clearSyncInfoBody() {
            this.syncInfoBody_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
        }

        private void removeSyncInfoBody(int i) {
            ensureSyncInfoBodyIsMutable();
            this.syncInfoBody_.remove(i);
        }

        public boolean hasSubHead() {
            return (this.bitField0_ & 2) != 0;
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead getSubHead() {
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead = this.subHead_;
            return syncInfoHead == null ? top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.getDefaultInstance() : syncInfoHead;
        }

        private void setSubHead(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead) {
            syncInfoHead.getClass();
            this.subHead_ = syncInfoHead;
            this.bitField0_ |= 2;
        }

        private void mergeSubHead(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead) {
            syncInfoHead.getClass();
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead syncInfoHead2 = this.subHead_;
            if (syncInfoHead2 != null && syncInfoHead2 != top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.getDefaultInstance()) {
                this.subHead_ = (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead) ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.Builder) top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoHead.newBuilder(this.subHead_).mergeFrom((com.google.protobuf.GeneratedMessageLite) syncInfoHead)).buildPartial();
            } else {
                this.subHead_ = syncInfoHead;
            }
            this.bitField0_ |= 2;
        }

        private void clearSubHead() {
            this.subHead_ = null;
            this.bitField0_ &= -3;
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder newBuilder() {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder) DEFAULT_INSTANCE.createBuilder();
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo) {
            return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder) DEFAULT_INSTANCE.createBuilder(syncRecallOperateInfo);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<SyncRecallOperateInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            public Builder() {
                super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.DEFAULT_INSTANCE);
            }

            public int getSyncInfoBodyCount() {
                return ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) this.instance).getSyncInfoBodyCount();
            }

            public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody getSyncInfoBody(int i) {
                return ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) this.instance).getSyncInfoBody(i);
            }

            public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder setSyncInfoBody(int i, top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody syncInfoBody) {
                copyOnWrite();
                ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) this.instance).setSyncInfoBody(i, syncInfoBody);
                return this;
            }
        }

        @Override
        public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
            int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
            switch (i) {
                case 1:
                    return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo();
                case 2:
                    return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder();
                case 3:
                    return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0003\u0000\u0001\u0003\u0005\u0003\u0000\u0001\u0000\u0003ဉ\u0000\u0004\u001b\u0005ဉ\u0001", new java.lang.Object[]{"bitField0_", "syncInfoHead_", "syncInfoBody_", top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.class, "subHead_"});
                case 4:
                    return DEFAULT_INSTANCE;
                case 5:
                    com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo> parser = PARSER;
                    if (parser == null) {
                        synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.class) {
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
            top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo();
            DEFAULT_INSTANCE = syncRecallOperateInfo;
            com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.class, syncRecallOperateInfo);
        }

        public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public int getType() {
        return this.type_;
    }

    private void setType(int i) {
        this.type_ = i;
    }

    private void clearType() {
        this.type_ = 0;
    }

    public int getPushId() {
        return this.pushId_;
    }

    private void setPushId(int i) {
        this.pushId_ = i;
    }

    private void clearPushId() {
        this.pushId_ = 0;
    }

    public boolean hasSyncContent() {
        return (this.bitField0_ & 1) != 0;
    }

    public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent getSyncContent() {
        top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent = this.syncContent_;
        return syncContent == null ? top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.getDefaultInstance() : syncContent;
    }

    private void setSyncContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent) {
        syncContent.getClass();
        this.syncContent_ = syncContent;
        this.bitField0_ |= 1;
    }

    private void mergeSyncContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent) {
        syncContent.getClass();
        top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent syncContent2 = this.syncContent_;
        if (syncContent2 != null && syncContent2 != top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.getDefaultInstance()) {
            this.syncContent_ = (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent) ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.Builder) top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncContent.newBuilder(this.syncContent_).mergeFrom((com.google.protobuf.GeneratedMessageLite) syncContent)).buildPartial();
        } else {
            this.syncContent_ = syncContent;
        }
        this.bitField0_ |= 1;
    }

    private void clearSyncContent() {
        this.syncContent_ = null;
        this.bitField0_ &= -2;
    }

    public boolean hasSyncRecallContent() {
        return (this.bitField0_ & 2) != 0;
    }

    public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo getSyncRecallContent() {
        top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo = this.syncRecallContent_;
        return syncRecallOperateInfo == null ? top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.getDefaultInstance() : syncRecallOperateInfo;
    }

    public void setSyncRecallContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo) {
        syncRecallOperateInfo.getClass();
        this.syncRecallContent_ = syncRecallOperateInfo;
        this.bitField0_ |= 2;
    }

    private void mergeSyncRecallContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo) {
        syncRecallOperateInfo.getClass();
        top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo2 = this.syncRecallContent_;
        if (syncRecallOperateInfo2 != null && syncRecallOperateInfo2 != top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.getDefaultInstance()) {
            this.syncRecallContent_ = (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo) ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder) top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.newBuilder(this.syncRecallContent_).mergeFrom((com.google.protobuf.GeneratedMessageLite) syncRecallOperateInfo)).buildPartial();
        } else {
            this.syncRecallContent_ = syncRecallOperateInfo;
        }
        this.bitField0_ |= 2;
    }

    private void clearSyncRecallContent() {
        this.syncRecallContent_ = null;
        this.bitField0_ &= -3;
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder newBuilder() {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder) DEFAULT_INSTANCE.createBuilder();
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder newBuilder(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush infoSyncPushOuterClass$InfoSyncPush) {
        return (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder) DEFAULT_INSTANCE.createBuilder(infoSyncPushOuterClass$InfoSyncPush);
    }

    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<InfoSyncPushOuterClass$InfoSyncPush, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        public Builder() {
            super(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.DEFAULT_INSTANCE);
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo getSyncRecallContent() {
            return ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) this.instance).getSyncRecallContent();
        }

        public top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder setSyncRecallContent(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo syncRecallOperateInfo) {
            copyOnWrite();
            ((top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush) this.instance).setSyncRecallContent(syncRecallOperateInfo);
            return this;
        }
    }

    @Override
    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
        int i = top.artmoe.inao.entries.InfoSyncPushOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
        switch (i) {
            case 1:
                return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush();
            case 2:
                return new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.Builder();
            case 3:
                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0004\u0000\u0001\u0003\b\u0004\u0000\u0000\u0000\u0003\u0004\u0004\u0004\u0007ဉ\u0000\bဉ\u0001", new java.lang.Object[]{"bitField0_", "type_", "pushId_", "syncContent_", "syncRecallContent_"});
            case 4:
                return DEFAULT_INSTANCE;
            case 5:
                com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush> parser = PARSER;
                if (parser == null) {
                    synchronized (top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.class) {
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
        top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush infoSyncPushOuterClass$InfoSyncPush = new top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush();
        DEFAULT_INSTANCE = infoSyncPushOuterClass$InfoSyncPush;
        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush.class, infoSyncPushOuterClass$InfoSyncPush);
    }

    public static top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush> parser() {
        return DEFAULT_INSTANCE.getParserForType();
    }
}
