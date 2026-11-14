package top.artmoe.inao.entries;

public final class QQMessageOuterClass$QQMessage extends com.google.protobuf.GeneratedMessageLite<QQMessageOuterClass$QQMessage, QQMessageOuterClass$QQMessage.Builder> implements top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder {
    private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage DEFAULT_INSTANCE;
    public static final int MESSAGEBODY_FIELD_NUMBER = 3;
    public static final int MESSAGECONTENTINFO_FIELD_NUMBER = 2;
    public static final int MESSAGEHEAD_FIELD_NUMBER = 1;
    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> PARSER;
    private int bitField0_;
    private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody_;
    private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo_;
    private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead_;

    private QQMessageOuterClass$QQMessage() {
    }

    public static final class MessageHead extends com.google.protobuf.GeneratedMessageLite<MessageHead, MessageHead.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead DEFAULT_INSTANCE;
        private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead> PARSER = null;
        public static final int RECEIVERPEERID_FIELD_NUMBER = 5;
        public static final int RECEIVERUID_FIELD_NUMBER = 6;
        public static final int SENDERINFO_FIELD_NUMBER = 8;
        public static final int SENDERPEERID_FIELD_NUMBER = 1;
        public static final int SENDERUID_FIELD_NUMBER = 2;
        private int bitField0_;
        private long receiverPeerId_;
        private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo_;
        private long senderPeerId_;
        private java.lang.String senderUid_ = "";
        private java.lang.String receiverUid_ = "";

        private MessageHead() {
        }

        public static final class SenderInfo extends com.google.protobuf.GeneratedMessageLite<SenderInfo, SenderInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo DEFAULT_INSTANCE;
            public static final int MSGSUBTYPE_FIELD_NUMBER = 2;
            public static final int NICKNAME_FIELD_NUMBER = 4;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo> PARSER = null;
            public static final int PEERID_FIELD_NUMBER = 1;
            private int msgSubType_;
            private java.lang.String nickName_ = "";
            private long peerId_;

            private SenderInfo() {
            }

            public long getPeerId() {
                return this.peerId_;
            }

            private void setPeerId(long j) {
                this.peerId_ = j;
            }

            private void clearPeerId() {
                this.peerId_ = 0L;
            }

            public int getMsgSubType() {
                return this.msgSubType_;
            }

            private void setMsgSubType(int i) {
                this.msgSubType_ = i;
            }

            private void clearMsgSubType() {
                this.msgSubType_ = 0;
            }

            public java.lang.String getNickName() {
                return this.nickName_;
            }

            public com.google.protobuf.ByteString getNickNameBytes() {
                return com.google.protobuf.ByteString.copyFromUtf8(this.nickName_);
            }

            private void setNickName(java.lang.String str) {
                str.getClass();
                this.nickName_ = str;
            }

            private void clearNickName() {
                this.nickName_ = getDefaultInstance().getNickName();
            }

            private void setNickNameBytes(com.google.protobuf.ByteString byteString) {
                com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                this.nickName_ = byteString.toStringUtf8();
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder newBuilder() {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo) {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder) DEFAULT_INSTANCE.createBuilder(senderInfo);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<SenderInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.DEFAULT_INSTANCE);
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo();
                    case 2:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0003\u0000\u0000\u0001\u0004\u0003\u0000\u0000\u0000\u0001\u0002\u0002\u0004\u0004Ȉ", new java.lang.Object[]{"peerId_", "msgSubType_", "nickName_"});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.class) {
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
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo();
                DEFAULT_INSTANCE = senderInfo;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.class, senderInfo);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
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

        public long getReceiverPeerId() {
            return this.receiverPeerId_;
        }

        private void setReceiverPeerId(long j) {
            this.receiverPeerId_ = j;
        }

        private void clearReceiverPeerId() {
            this.receiverPeerId_ = 0L;
        }

        public java.lang.String getReceiverUid() {
            return this.receiverUid_;
        }

        public com.google.protobuf.ByteString getReceiverUidBytes() {
            return com.google.protobuf.ByteString.copyFromUtf8(this.receiverUid_);
        }

        private void setReceiverUid(java.lang.String str) {
            str.getClass();
            this.receiverUid_ = str;
        }

        private void clearReceiverUid() {
            this.receiverUid_ = getDefaultInstance().getReceiverUid();
        }

        private void setReceiverUidBytes(com.google.protobuf.ByteString byteString) {
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
            this.receiverUid_ = byteString.toStringUtf8();
        }

        public boolean hasSenderInfo() {
            return (this.bitField0_ & 1) != 0;
        }

        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo getSenderInfo() {
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo = this.senderInfo_;
            return senderInfo == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.getDefaultInstance() : senderInfo;
        }

        private void setSenderInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo) {
            senderInfo.getClass();
            this.senderInfo_ = senderInfo;
            this.bitField0_ |= 1;
        }

        private void mergeSenderInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo) {
            senderInfo.getClass();
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo senderInfo2 = this.senderInfo_;
            if (senderInfo2 != null && senderInfo2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.getDefaultInstance()) {
                this.senderInfo_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.SenderInfo.newBuilder(this.senderInfo_).mergeFrom((com.google.protobuf.GeneratedMessageLite) senderInfo)).buildPartial();
            } else {
                this.senderInfo_ = senderInfo;
            }
            this.bitField0_ |= 1;
        }

        private void clearSenderInfo() {
            this.senderInfo_ = null;
            this.bitField0_ &= -2;
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder newBuilder() {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder) DEFAULT_INSTANCE.createBuilder();
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead) {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder) DEFAULT_INSTANCE.createBuilder(messageHead);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MessageHead, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            public Builder() {
                super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.DEFAULT_INSTANCE);
            }
        }

        @Override
        public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
            int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
            switch (i) {
                case 1:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead();
                case 2:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder();
                case 3:
                    return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0005\u0000\u0001\u0001\b\u0005\u0000\u0000\u0000\u0001\u0002\u0002Ȉ\u0005\u0002\u0006Ȉ\bဉ\u0000", new java.lang.Object[]{"bitField0_", "senderPeerId_", "senderUid_", "receiverPeerId_", "receiverUid_", "senderInfo_"});
                case 4:
                    return DEFAULT_INSTANCE;
                case 5:
                    com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead> parser = PARSER;
                    if (parser == null) {
                        synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.class) {
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
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead();
            DEFAULT_INSTANCE = messageHead;
            com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.class, messageHead);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class MessageContentInfo extends com.google.protobuf.GeneratedMessageLite<MessageContentInfo, MessageContentInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo DEFAULT_INSTANCE;
        public static final int MSGSEQ_FIELD_NUMBER = 5;
        public static final int MSGSUBTYPE_FIELD_NUMBER = 2;
        public static final int MSGTIME_FIELD_NUMBER = 6;
        public static final int MSGTYPE_FIELD_NUMBER = 1;
        public static final int MSG_UID_FIELD_NUMBER = 12;
        private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo> PARSER = null;
        public static final int SUBSEQ_FIELD_NUMBER = 3;
        private int msgSeq_;
        private int msgSubType_;
        private long msgTime_;
        private int msgType_;
        private long msgUid_;
        private int subSeq_;

        private MessageContentInfo() {
        }

        public int getMsgType() {
            return this.msgType_;
        }

        private void setMsgType(int i) {
            this.msgType_ = i;
        }

        private void clearMsgType() {
            this.msgType_ = 0;
        }

        public int getMsgSubType() {
            return this.msgSubType_;
        }

        private void setMsgSubType(int i) {
            this.msgSubType_ = i;
        }

        private void clearMsgSubType() {
            this.msgSubType_ = 0;
        }

        public int getSubSeq() {
            return this.subSeq_;
        }

        private void setSubSeq(int i) {
            this.subSeq_ = i;
        }

        private void clearSubSeq() {
            this.subSeq_ = 0;
        }

        public int getMsgSeq() {
            return this.msgSeq_;
        }

        private void setMsgSeq(int i) {
            this.msgSeq_ = i;
        }

        private void clearMsgSeq() {
            this.msgSeq_ = 0;
        }

        public long getMsgTime() {
            return this.msgTime_;
        }

        private void setMsgTime(long j) {
            this.msgTime_ = j;
        }

        private void clearMsgTime() {
            this.msgTime_ = 0L;
        }

        public long getMsgUid() {
            return this.msgUid_;
        }

        private void setMsgUid(long j) {
            this.msgUid_ = j;
        }

        private void clearMsgUid() {
            this.msgUid_ = 0L;
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder newBuilder() {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder) DEFAULT_INSTANCE.createBuilder();
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo) {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder) DEFAULT_INSTANCE.createBuilder(messageContentInfo);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MessageContentInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            public Builder() {
                super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.DEFAULT_INSTANCE);
            }
        }

        @Override
        public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
            int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
            switch (i) {
                case 1:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo();
                case 2:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder();
                case 3:
                    return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0006\u0000\u0000\u0001\f\u0006\u0000\u0000\u0000\u0001\u0004\u0002\u0004\u0003\u0004\u0005\u0004\u0006\u0002\f\u0003", new java.lang.Object[]{"msgType_", "msgSubType_", "subSeq_", "msgSeq_", "msgTime_", "msgUid_"});
                case 4:
                    return DEFAULT_INSTANCE;
                case 5:
                    com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo> parser = PARSER;
                    if (parser == null) {
                        synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.class) {
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
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo();
            DEFAULT_INSTANCE = messageContentInfo;
            com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.class, messageContentInfo);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public static final class MessageBody extends com.google.protobuf.GeneratedMessageLite<MessageBody, MessageBody.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
        private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody DEFAULT_INSTANCE;
        public static final int OPERATIONINFO_FIELD_NUMBER = 2;
        private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody> PARSER = null;
        public static final int RICHMSG_FIELD_NUMBER = 1;
        private int bitField0_;
        private com.google.protobuf.ByteString operationInfo_ = com.google.protobuf.ByteString.EMPTY;
        private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg_;

        private MessageBody() {
        }

        public static final class RichMsg extends com.google.protobuf.GeneratedMessageLite<RichMsg, RichMsg.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg DEFAULT_INSTANCE;
            public static final int MSGCONTENT_FIELD_NUMBER = 2;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg> PARSER;
            private com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> msgContent_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();

            public interface MsgContentOrBuilder extends com.google.protobuf.MessageLiteOrBuilder {
            }

            private RichMsg() {
            }

            public static final class MsgContent extends com.google.protobuf.GeneratedMessageLite<MsgContent, MsgContent.Builder> implements top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContentOrBuilder {
                private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent DEFAULT_INSTANCE;
                public static final int MSGSENDER_FIELD_NUMBER = 16;
                private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> PARSER = null;
                public static final int TEXTMSG_FIELD_NUMBER = 1;
                private int bitField0_;
                private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender_;
                private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg_;

                private MsgContent() {
                }

                public static final class TextMsg extends com.google.protobuf.GeneratedMessageLite<TextMsg, TextMsg.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                    private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg DEFAULT_INSTANCE;
                    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg> PARSER = null;
                    public static final int TEXT_FIELD_NUMBER = 1;
                    private java.lang.String text_ = "";

                    private TextMsg() {
                    }

                    public java.lang.String getText() {
                        return this.text_;
                    }

                    public com.google.protobuf.ByteString getTextBytes() {
                        return com.google.protobuf.ByteString.copyFromUtf8(this.text_);
                    }

                    public void setText(java.lang.String str) {
                        str.getClass();
                        this.text_ = str;
                    }

                    private void clearText() {
                        this.text_ = getDefaultInstance().getText();
                    }

                    private void setTextBytes(com.google.protobuf.ByteString byteString) {
                        com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                        this.text_ = byteString.toStringUtf8();
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder newBuilder() {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder) DEFAULT_INSTANCE.createBuilder();
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg) {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder) DEFAULT_INSTANCE.createBuilder(textMsg);
                    }

                    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<TextMsg, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                        public Builder() {
                            super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.DEFAULT_INSTANCE);
                        }

                        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder setText(java.lang.String str) {
                            copyOnWrite();
                            ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) this.instance).setText(str);
                            return this;
                        }
                    }

                    @Override
                    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                        int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                        switch (i) {
                            case 1:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg();
                            case 2:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder();
                            case 3:
                                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001Ȉ", new java.lang.Object[]{"text_"});
                            case 4:
                                return DEFAULT_INSTANCE;
                            case 5:
                                com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg> parser = PARSER;
                                if (parser == null) {
                                    synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.class) {
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
                        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg();
                        DEFAULT_INSTANCE = textMsg;
                        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.class, textMsg);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg getDefaultInstance() {
                        return DEFAULT_INSTANCE;
                    }

                    public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg> parser() {
                        return DEFAULT_INSTANCE.getParserForType();
                    }
                }

                public static final class MsgSender extends com.google.protobuf.GeneratedMessageLite<MsgSender, MsgSender.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                    private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender DEFAULT_INSTANCE;
                    public static final int NICKNAME_FIELD_NUMBER = 1;
                    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender> PARSER;
                    private java.lang.String nickName_ = "";

                    private MsgSender() {
                    }

                    public java.lang.String getNickName() {
                        return this.nickName_;
                    }

                    public com.google.protobuf.ByteString getNickNameBytes() {
                        return com.google.protobuf.ByteString.copyFromUtf8(this.nickName_);
                    }

                    private void setNickName(java.lang.String str) {
                        str.getClass();
                        this.nickName_ = str;
                    }

                    private void clearNickName() {
                        this.nickName_ = getDefaultInstance().getNickName();
                    }

                    private void setNickNameBytes(com.google.protobuf.ByteString byteString) {
                        com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                        this.nickName_ = byteString.toStringUtf8();
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder newBuilder() {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder) DEFAULT_INSTANCE.createBuilder();
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender) {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder) DEFAULT_INSTANCE.createBuilder(msgSender);
                    }

                    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MsgSender, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                        public Builder() {
                            super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.DEFAULT_INSTANCE);
                        }
                    }

                    @Override
                    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                        int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                        switch (i) {
                            case 1:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender();
                            case 2:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder();
                            case 3:
                                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001Ȉ", new java.lang.Object[]{"nickName_"});
                            case 4:
                                return DEFAULT_INSTANCE;
                            case 5:
                                com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender> parser = PARSER;
                                if (parser == null) {
                                    synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.class) {
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
                        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender();
                        DEFAULT_INSTANCE = msgSender;
                        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.class, msgSender);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender getDefaultInstance() {
                        return DEFAULT_INSTANCE;
                    }

                    public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender> parser() {
                        return DEFAULT_INSTANCE.getParserForType();
                    }
                }

                public boolean hasTextMsg() {
                    return (this.bitField0_ & 1) != 0;
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg getTextMsg() {
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg = this.textMsg_;
                    return textMsg == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.getDefaultInstance() : textMsg;
                }

                public void setTextMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg) {
                    textMsg.getClass();
                    this.textMsg_ = textMsg;
                    this.bitField0_ |= 1;
                }

                private void mergeTextMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg) {
                    textMsg.getClass();
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg2 = this.textMsg_;
                    if (textMsg2 != null && textMsg2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.getDefaultInstance()) {
                        this.textMsg_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg.newBuilder(this.textMsg_).mergeFrom((com.google.protobuf.GeneratedMessageLite) textMsg)).buildPartial();
                    } else {
                        this.textMsg_ = textMsg;
                    }
                    this.bitField0_ |= 1;
                }

                private void clearTextMsg() {
                    this.textMsg_ = null;
                    this.bitField0_ &= -2;
                }

                public boolean hasMsgSender() {
                    return (this.bitField0_ & 2) != 0;
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender getMsgSender() {
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender = this.msgSender_;
                    return msgSender == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.getDefaultInstance() : msgSender;
                }

                private void setMsgSender(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender) {
                    msgSender.getClass();
                    this.msgSender_ = msgSender;
                    this.bitField0_ |= 2;
                }

                private void mergeMsgSender(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender) {
                    msgSender.getClass();
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender msgSender2 = this.msgSender_;
                    if (msgSender2 != null && msgSender2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.getDefaultInstance()) {
                        this.msgSender_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.MsgSender.newBuilder(this.msgSender_).mergeFrom((com.google.protobuf.GeneratedMessageLite) msgSender)).buildPartial();
                    } else {
                        this.msgSender_ = msgSender;
                    }
                    this.bitField0_ |= 2;
                }

                private void clearMsgSender() {
                    this.msgSender_ = null;
                    this.bitField0_ &= -3;
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder newBuilder() {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder) DEFAULT_INSTANCE.createBuilder();
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent) {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder) DEFAULT_INSTANCE.createBuilder(msgContent);
                }

                public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MsgContent, Builder> implements top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContentOrBuilder {
                    public Builder() {
                        super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.DEFAULT_INSTANCE);
                    }

                    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg getTextMsg() {
                        return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) this.instance).getTextMsg();
                    }

                    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder setTextMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.TextMsg textMsg) {
                        copyOnWrite();
                        ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent) this.instance).setTextMsg(textMsg);
                        return this;
                    }
                }

                @Override
                public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                    int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                    switch (i) {
                        case 1:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent();
                        case 2:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.Builder();
                        case 3:
                            return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0002\u0000\u0001\u0001\u0010\u0002\u0000\u0000\u0000\u0001ဉ\u0000\u0010ဉ\u0001", new java.lang.Object[]{"bitField0_", "textMsg_", "msgSender_"});
                        case 4:
                            return DEFAULT_INSTANCE;
                        case 5:
                            com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> parser = PARSER;
                            if (parser == null) {
                                synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.class) {
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
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent();
                    DEFAULT_INSTANCE = msgContent;
                    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.class, msgContent);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent getDefaultInstance() {
                    return DEFAULT_INSTANCE;
                }

                public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> parser() {
                    return DEFAULT_INSTANCE.getParserForType();
                }
            }

            public java.util.List<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> getMsgContentList() {
                return this.msgContent_;
            }

            public java.util.List<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContentOrBuilder> getMsgContentOrBuilderList() {
                return this.msgContent_;
            }

            public int getMsgContentCount() {
                return this.msgContent_.size();
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent getMsgContent(int i) {
                return this.msgContent_.get(i);
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContentOrBuilder getMsgContentOrBuilder(int i) {
                return this.msgContent_.get(i);
            }

            private void ensureMsgContentIsMutable() {
                com.google.protobuf.Internal.ProtobufList<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> protobufList = this.msgContent_;
                if (protobufList.isModifiable()) {
                    return;
                }
                this.msgContent_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(protobufList);
            }

            public void setMsgContent(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent) {
                msgContent.getClass();
                ensureMsgContentIsMutable();
                this.msgContent_.set(i, msgContent);
            }

            private void addMsgContent(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent) {
                msgContent.getClass();
                ensureMsgContentIsMutable();
                this.msgContent_.add(msgContent);
            }

            private void addMsgContent(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent) {
                msgContent.getClass();
                ensureMsgContentIsMutable();
                this.msgContent_.add(i, msgContent);
            }

            private void addAllMsgContent(java.lang.Iterable<? extends top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent> iterable) {
                ensureMsgContentIsMutable();
                com.google.protobuf.AbstractMessageLite.addAll(iterable, this.msgContent_);
            }

            private void clearMsgContent() {
                this.msgContent_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
            }

            private void removeMsgContent(int i) {
                ensureMsgContentIsMutable();
                this.msgContent_.remove(i);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder newBuilder() {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg) {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder) DEFAULT_INSTANCE.createBuilder(richMsg);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<RichMsg, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.DEFAULT_INSTANCE);
                }

                public java.util.List getMsgContentList() {
                    return java.util.Collections.unmodifiableList(((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) this.instance).getMsgContentList());
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder setMsgContent(int i, top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent msgContent) {
                    copyOnWrite();
                    ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) this.instance).setMsgContent(i, msgContent);
                    return this;
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg();
                    case 2:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0000\u0002\u0002\u0001\u0000\u0001\u0000\u0002\u001b", new java.lang.Object[]{"msgContent_", top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.MsgContent.class});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.class) {
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
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg();
                DEFAULT_INSTANCE = richMsg;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.class, richMsg);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public static final class GroupRecallOperationInfo extends com.google.protobuf.GeneratedMessageLite<GroupRecallOperationInfo, GroupRecallOperationInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo DEFAULT_INSTANCE;
            public static final int INFO_FIELD_NUMBER = 11;
            public static final int MSGSEQ_FIELD_NUMBER = 37;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo> PARSER = null;
            public static final int PEERID_FIELD_NUMBER = 4;
            private int bitField0_;
            private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info_;
            private int msgSeq_;
            private long peerId_;

            private GroupRecallOperationInfo() {
            }

            public static final class Info extends com.google.protobuf.GeneratedMessageLite<Info, Info.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info DEFAULT_INSTANCE;
                public static final int MSGINFO_FIELD_NUMBER = 3;
                public static final int OPERATORUID_FIELD_NUMBER = 1;
                private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info> PARSER;
                private int bitField0_;
                private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo_;
                private java.lang.String operatorUid_ = "";

                private Info() {
                }

                public static final class MsgInfo extends com.google.protobuf.GeneratedMessageLite<MsgInfo, MsgInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                    private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo DEFAULT_INSTANCE;
                    public static final int MSGSEQ_FIELD_NUMBER = 1;
                    public static final int MSGTIME_FIELD_NUMBER = 2;
                    private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo> PARSER = null;
                    public static final int SENDERUID_FIELD_NUMBER = 6;
                    private int msgSeq_;
                    private long msgTime_;
                    private java.lang.String senderUid_ = "";

                    private MsgInfo() {
                    }

                    public int getMsgSeq() {
                        return this.msgSeq_;
                    }

                    public void setMsgSeq(int i) {
                        this.msgSeq_ = i;
                    }

                    private void clearMsgSeq() {
                        this.msgSeq_ = 0;
                    }

                    public long getMsgTime() {
                        return this.msgTime_;
                    }

                    private void setMsgTime(long j) {
                        this.msgTime_ = j;
                    }

                    private void clearMsgTime() {
                        this.msgTime_ = 0L;
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

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder newBuilder() {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder) DEFAULT_INSTANCE.createBuilder();
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo) {
                        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder) DEFAULT_INSTANCE.createBuilder(msgInfo);
                    }

                    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MsgInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                        public Builder() {
                            super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.DEFAULT_INSTANCE);
                        }

                        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder setMsgSeq(int i) {
                            copyOnWrite();
                            ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) this.instance).setMsgSeq(i);
                            return this;
                        }
                    }

                    @Override
                    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                        int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                        switch (i) {
                            case 1:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo();
                            case 2:
                                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder();
                            case 3:
                                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0003\u0000\u0000\u0001\u0006\u0003\u0000\u0000\u0000\u0001\u0004\u0002\u0002\u0006Ȉ", new java.lang.Object[]{"msgSeq_", "msgTime_", "senderUid_"});
                            case 4:
                                return DEFAULT_INSTANCE;
                            case 5:
                                com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo> parser = PARSER;
                                if (parser == null) {
                                    synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.class) {
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
                        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo();
                        DEFAULT_INSTANCE = msgInfo;
                        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.class, msgInfo);
                    }

                    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo getDefaultInstance() {
                        return DEFAULT_INSTANCE;
                    }

                    public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo> parser() {
                        return DEFAULT_INSTANCE.getParserForType();
                    }
                }

                public java.lang.String getOperatorUid() {
                    return this.operatorUid_;
                }

                public com.google.protobuf.ByteString getOperatorUidBytes() {
                    return com.google.protobuf.ByteString.copyFromUtf8(this.operatorUid_);
                }

                private void setOperatorUid(java.lang.String str) {
                    str.getClass();
                    this.operatorUid_ = str;
                }

                private void clearOperatorUid() {
                    this.operatorUid_ = getDefaultInstance().getOperatorUid();
                }

                private void setOperatorUidBytes(com.google.protobuf.ByteString byteString) {
                    com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                    this.operatorUid_ = byteString.toStringUtf8();
                }

                public boolean hasMsgInfo() {
                    return (this.bitField0_ & 1) != 0;
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo getMsgInfo() {
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo = this.msgInfo_;
                    return msgInfo == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.getDefaultInstance() : msgInfo;
                }

                public void setMsgInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo) {
                    msgInfo.getClass();
                    this.msgInfo_ = msgInfo;
                    this.bitField0_ |= 1;
                }

                private void mergeMsgInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo) {
                    msgInfo.getClass();
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo2 = this.msgInfo_;
                    if (msgInfo2 != null && msgInfo2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.getDefaultInstance()) {
                        this.msgInfo_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo.newBuilder(this.msgInfo_).mergeFrom((com.google.protobuf.GeneratedMessageLite) msgInfo)).buildPartial();
                    } else {
                        this.msgInfo_ = msgInfo;
                    }
                    this.bitField0_ |= 1;
                }

                private void clearMsgInfo() {
                    this.msgInfo_ = null;
                    this.bitField0_ &= -2;
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder newBuilder() {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder) DEFAULT_INSTANCE.createBuilder();
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info) {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder) DEFAULT_INSTANCE.createBuilder(info);
                }

                public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<Info, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                    public Builder() {
                        super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.DEFAULT_INSTANCE);
                    }

                    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo getMsgInfo() {
                        return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) this.instance).getMsgInfo();
                    }

                    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder setMsgInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.MsgInfo msgInfo) {
                        copyOnWrite();
                        ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) this.instance).setMsgInfo(msgInfo);
                        return this;
                    }
                }

                @Override
                public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                    int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                    switch (i) {
                        case 1:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info();
                        case 2:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder();
                        case 3:
                            return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0002\u0000\u0001\u0001\u0003\u0002\u0000\u0000\u0000\u0001Ȉ\u0003ဉ\u0000", new java.lang.Object[]{"bitField0_", "operatorUid_", "msgInfo_"});
                        case 4:
                            return DEFAULT_INSTANCE;
                        case 5:
                            com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info> parser = PARSER;
                            if (parser == null) {
                                synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.class) {
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
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info();
                    DEFAULT_INSTANCE = info;
                    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.class, info);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info getDefaultInstance() {
                    return DEFAULT_INSTANCE;
                }

                public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info> parser() {
                    return DEFAULT_INSTANCE.getParserForType();
                }
            }

            public long getPeerId() {
                return this.peerId_;
            }

            private void setPeerId(long j) {
                this.peerId_ = j;
            }

            private void clearPeerId() {
                this.peerId_ = 0L;
            }

            public boolean hasInfo() {
                return (this.bitField0_ & 1) != 0;
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info getInfo() {
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info = this.info_;
                return info == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.getDefaultInstance() : info;
            }

            public void setInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info) {
                info.getClass();
                this.info_ = info;
                this.bitField0_ |= 1;
            }

            private void mergeInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info) {
                info.getClass();
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info2 = this.info_;
                if (info2 != null && info2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.getDefaultInstance()) {
                    this.info_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.newBuilder(this.info_).mergeFrom((com.google.protobuf.GeneratedMessageLite) info)).buildPartial();
                } else {
                    this.info_ = info;
                }
                this.bitField0_ |= 1;
            }

            private void clearInfo() {
                this.info_ = null;
                this.bitField0_ &= -2;
            }

            public int getMsgSeq() {
                return this.msgSeq_;
            }

            public void setMsgSeq(int i) {
                this.msgSeq_ = i;
            }

            private void clearMsgSeq() {
                this.msgSeq_ = 0;
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder newBuilder() {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo groupRecallOperationInfo) {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder) DEFAULT_INSTANCE.createBuilder(groupRecallOperationInfo);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<GroupRecallOperationInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.DEFAULT_INSTANCE);
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info getInfo() {
                    return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) this.instance).getInfo();
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder setInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info info) {
                    copyOnWrite();
                    ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) this.instance).setInfo(info);
                    return this;
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder setMsgSeq(int i) {
                    copyOnWrite();
                    ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo) this.instance).setMsgSeq(i);
                    return this;
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo();
                    case 2:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0003\u0000\u0001\u0004%\u0003\u0000\u0000\u0000\u0004\u0002\u000bဉ\u0000%\u0004", new java.lang.Object[]{"bitField0_", "peerId_", "info_", "msgSeq_"});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.class) {
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
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo groupRecallOperationInfo = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo();
                DEFAULT_INSTANCE = groupRecallOperationInfo;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.class, groupRecallOperationInfo);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public static final class C2CRecallOperationInfo extends com.google.protobuf.GeneratedMessageLite<C2CRecallOperationInfo, C2CRecallOperationInfo.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo DEFAULT_INSTANCE;
            public static final int INFO_FIELD_NUMBER = 1;
            private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo> PARSER;
            private int bitField0_;
            private top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info_;

            private C2CRecallOperationInfo() {
            }

            public static final class Info extends com.google.protobuf.GeneratedMessageLite<Info, Info.Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                private static final top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info DEFAULT_INSTANCE;
                public static final int MSGRANDOM_FIELD_NUMBER = 6;
                public static final int MSGSEQ_FIELD_NUMBER = 20;
                public static final int MSGTIME_FIELD_NUMBER = 5;
                public static final int OPERATORUID_FIELD_NUMBER = 1;
                private static volatile com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info> PARSER = null;
                public static final int RECEIVERUID_FIELD_NUMBER = 2;
                private long msgRandom_;
                private int msgSeq_;
                private long msgTime_;
                private java.lang.String operatorUid_ = "";
                private java.lang.String receiverUid_ = "";

                private Info() {
                }

                public java.lang.String getOperatorUid() {
                    return this.operatorUid_;
                }

                public com.google.protobuf.ByteString getOperatorUidBytes() {
                    return com.google.protobuf.ByteString.copyFromUtf8(this.operatorUid_);
                }

                private void setOperatorUid(java.lang.String str) {
                    str.getClass();
                    this.operatorUid_ = str;
                }

                private void clearOperatorUid() {
                    this.operatorUid_ = getDefaultInstance().getOperatorUid();
                }

                private void setOperatorUidBytes(com.google.protobuf.ByteString byteString) {
                    com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                    this.operatorUid_ = byteString.toStringUtf8();
                }

                public java.lang.String getReceiverUid() {
                    return this.receiverUid_;
                }

                public com.google.protobuf.ByteString getReceiverUidBytes() {
                    return com.google.protobuf.ByteString.copyFromUtf8(this.receiverUid_);
                }

                private void setReceiverUid(java.lang.String str) {
                    str.getClass();
                    this.receiverUid_ = str;
                }

                private void clearReceiverUid() {
                    this.receiverUid_ = getDefaultInstance().getReceiverUid();
                }

                private void setReceiverUidBytes(com.google.protobuf.ByteString byteString) {
                    com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(byteString);
                    this.receiverUid_ = byteString.toStringUtf8();
                }

                public long getMsgTime() {
                    return this.msgTime_;
                }

                private void setMsgTime(long j) {
                    this.msgTime_ = j;
                }

                private void clearMsgTime() {
                    this.msgTime_ = 0L;
                }

                public long getMsgRandom() {
                    return this.msgRandom_;
                }

                private void setMsgRandom(long j) {
                    this.msgRandom_ = j;
                }

                private void clearMsgRandom() {
                    this.msgRandom_ = 0L;
                }

                public int getMsgSeq() {
                    return this.msgSeq_;
                }

                public void setMsgSeq(int i) {
                    this.msgSeq_ = i;
                }

                private void clearMsgSeq() {
                    this.msgSeq_ = 0;
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder newBuilder() {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder) DEFAULT_INSTANCE.createBuilder();
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info) {
                    return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder) DEFAULT_INSTANCE.createBuilder(info);
                }

                public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<Info, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                    public Builder() {
                        super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.DEFAULT_INSTANCE);
                    }

                    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder setMsgSeq(int i) {
                        copyOnWrite();
                        ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) this.instance).setMsgSeq(i);
                        return this;
                    }
                }

                @Override
                public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                    int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                    switch (i) {
                        case 1:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info();
                        case 2:
                            return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder();
                        case 3:
                            return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0005\u0000\u0000\u0001\u0014\u0005\u0000\u0000\u0000\u0001Ȉ\u0002Ȉ\u0005\u0002\u0006\u0002\u0014\u0004", new java.lang.Object[]{"operatorUid_", "receiverUid_", "msgTime_", "msgRandom_", "msgSeq_"});
                        case 4:
                            return DEFAULT_INSTANCE;
                        case 5:
                            com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info> parser = PARSER;
                            if (parser == null) {
                                synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.class) {
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
                    top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info();
                    DEFAULT_INSTANCE = info;
                    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.class, info);
                }

                public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info getDefaultInstance() {
                    return DEFAULT_INSTANCE;
                }

                public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info> parser() {
                    return DEFAULT_INSTANCE.getParserForType();
                }
            }

            public boolean hasInfo() {
                return (this.bitField0_ & 1) != 0;
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info getInfo() {
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info = this.info_;
                return info == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.getDefaultInstance() : info;
            }

            public void setInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info) {
                info.getClass();
                this.info_ = info;
                this.bitField0_ |= 1;
            }

            private void mergeInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info) {
                info.getClass();
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info2 = this.info_;
                if (info2 != null && info2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.getDefaultInstance()) {
                    this.info_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.newBuilder(this.info_).mergeFrom((com.google.protobuf.GeneratedMessageLite) info)).buildPartial();
                } else {
                    this.info_ = info;
                }
                this.bitField0_ |= 1;
            }

            private void clearInfo() {
                this.info_ = null;
                this.bitField0_ &= -2;
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder newBuilder() {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder) DEFAULT_INSTANCE.createBuilder();
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo c2CRecallOperationInfo) {
                return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder) DEFAULT_INSTANCE.createBuilder(c2CRecallOperationInfo);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<C2CRecallOperationInfo, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
                public Builder() {
                    super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.DEFAULT_INSTANCE);
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info getInfo() {
                    return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) this.instance).getInfo();
                }

                public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder setInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info info) {
                    copyOnWrite();
                    ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo) this.instance).setInfo(info);
                    return this;
                }
            }

            @Override
            public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
                int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
                switch (i) {
                    case 1:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo();
                    case 2:
                        return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder();
                    case 3:
                        return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0000\u0000\u0000\u0001ဉ\u0000", new java.lang.Object[]{"bitField0_", "info_"});
                    case 4:
                        return DEFAULT_INSTANCE;
                    case 5:
                        com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo> parser = PARSER;
                        if (parser == null) {
                            synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.class) {
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
                top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo c2CRecallOperationInfo = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo();
                DEFAULT_INSTANCE = c2CRecallOperationInfo;
                com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.class, c2CRecallOperationInfo);
            }

            public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo> parser() {
                return DEFAULT_INSTANCE.getParserForType();
            }
        }

        public boolean hasRichMsg() {
            return (this.bitField0_ & 1) != 0;
        }

        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg getRichMsg() {
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg = this.richMsg_;
            return richMsg == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.getDefaultInstance() : richMsg;
        }

        public void setRichMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg) {
            richMsg.getClass();
            this.richMsg_ = richMsg;
            this.bitField0_ |= 1;
        }

        private void mergeRichMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg) {
            richMsg.getClass();
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg2 = this.richMsg_;
            if (richMsg2 != null && richMsg2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.getDefaultInstance()) {
                this.richMsg_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg.newBuilder(this.richMsg_).mergeFrom((com.google.protobuf.GeneratedMessageLite) richMsg)).buildPartial();
            } else {
                this.richMsg_ = richMsg;
            }
            this.bitField0_ |= 1;
        }

        private void clearRichMsg() {
            this.richMsg_ = null;
            this.bitField0_ &= -2;
        }

        public com.google.protobuf.ByteString getOperationInfo() {
            return this.operationInfo_;
        }

        public void setOperationInfo(com.google.protobuf.ByteString byteString) {
            byteString.getClass();
            this.operationInfo_ = byteString;
        }

        private void clearOperationInfo() {
            this.operationInfo_ = getDefaultInstance().getOperationInfo();
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder newBuilder() {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder) DEFAULT_INSTANCE.createBuilder();
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody) {
            return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder) DEFAULT_INSTANCE.createBuilder(messageBody);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<MessageBody, Builder> implements com.google.protobuf.MessageLiteOrBuilder {
            public Builder() {
                super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.DEFAULT_INSTANCE);
            }

            public boolean hasRichMsg() {
                return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) this.instance).hasRichMsg();
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg getRichMsg() {
                return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) this.instance).getRichMsg();
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder setRichMsg(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.RichMsg richMsg) {
                copyOnWrite();
                ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) this.instance).setRichMsg(richMsg);
                return this;
            }

            public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder setOperationInfo(com.google.protobuf.ByteString byteString) {
                copyOnWrite();
                ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) this.instance).setOperationInfo(byteString);
                return this;
            }
        }

        @Override
        public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
            int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
            switch (i) {
                case 1:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody();
                case 2:
                    return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder();
                case 3:
                    return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0002\u0000\u0001\u0001\u0002\u0002\u0000\u0000\u0000\u0001ဉ\u0000\u0002\n", new java.lang.Object[]{"bitField0_", "richMsg_", "operationInfo_"});
                case 4:
                    return DEFAULT_INSTANCE;
                case 5:
                    com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody> parser = PARSER;
                    if (parser == null) {
                        synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.class) {
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
            top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody();
            DEFAULT_INSTANCE = messageBody;
            com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.class, messageBody);
        }

        public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody> parser() {
            return DEFAULT_INSTANCE.getParserForType();
        }
    }

    public boolean hasMessageHead() {
        return (this.bitField0_ & 1) != 0;
    }

    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead getMessageHead() {
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead = this.messageHead_;
        return messageHead == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.getDefaultInstance() : messageHead;
    }

    private void setMessageHead(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead) {
        messageHead.getClass();
        this.messageHead_ = messageHead;
        this.bitField0_ |= 1;
    }

    private void mergeMessageHead(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead) {
        messageHead.getClass();
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead messageHead2 = this.messageHead_;
        if (messageHead2 != null && messageHead2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.getDefaultInstance()) {
            this.messageHead_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageHead.newBuilder(this.messageHead_).mergeFrom((com.google.protobuf.GeneratedMessageLite) messageHead)).buildPartial();
        } else {
            this.messageHead_ = messageHead;
        }
        this.bitField0_ |= 1;
    }

    private void clearMessageHead() {
        this.messageHead_ = null;
        this.bitField0_ &= -2;
    }

    public boolean hasMessageContentInfo() {
        return (this.bitField0_ & 2) != 0;
    }

    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo getMessageContentInfo() {
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo = this.messageContentInfo_;
        return messageContentInfo == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.getDefaultInstance() : messageContentInfo;
    }

    private void setMessageContentInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo) {
        messageContentInfo.getClass();
        this.messageContentInfo_ = messageContentInfo;
        this.bitField0_ |= 2;
    }

    private void mergeMessageContentInfo(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo) {
        messageContentInfo.getClass();
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo messageContentInfo2 = this.messageContentInfo_;
        if (messageContentInfo2 != null && messageContentInfo2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.getDefaultInstance()) {
            this.messageContentInfo_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageContentInfo.newBuilder(this.messageContentInfo_).mergeFrom((com.google.protobuf.GeneratedMessageLite) messageContentInfo)).buildPartial();
        } else {
            this.messageContentInfo_ = messageContentInfo;
        }
        this.bitField0_ |= 2;
    }

    private void clearMessageContentInfo() {
        this.messageContentInfo_ = null;
        this.bitField0_ &= -3;
    }

    public boolean hasMessageBody() {
        return (this.bitField0_ & 4) != 0;
    }

    public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody getMessageBody() {
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody = this.messageBody_;
        return messageBody == null ? top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.getDefaultInstance() : messageBody;
    }

    public void setMessageBody(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody) {
        messageBody.getClass();
        this.messageBody_ = messageBody;
        this.bitField0_ |= 4;
    }

    private void mergeMessageBody(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody) {
        messageBody.getClass();
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody2 = this.messageBody_;
        if (messageBody2 != null && messageBody2 != top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.getDefaultInstance()) {
            this.messageBody_ = (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody) ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.Builder) top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody.newBuilder(this.messageBody_).mergeFrom((com.google.protobuf.GeneratedMessageLite) messageBody)).buildPartial();
        } else {
            this.messageBody_ = messageBody;
        }
        this.bitField0_ |= 4;
    }

    private void clearMessageBody() {
        this.messageBody_ = null;
        this.bitField0_ &= -5;
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(java.nio.ByteBuffer byteBuffer) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(java.nio.ByteBuffer byteBuffer, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteBuffer, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(com.google.protobuf.ByteString byteString) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(com.google.protobuf.ByteString byteString, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, byteString, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(byte[] bArr) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(byte[] bArr, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, bArr, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseDelimitedFrom(java.io.InputStream inputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseDelimitedFrom(java.io.InputStream inputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseDelimitedFrom(DEFAULT_INSTANCE, inputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(com.google.protobuf.CodedInputStream codedInputStream) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage parseFrom(com.google.protobuf.CodedInputStream codedInputStream, com.google.protobuf.ExtensionRegistryLite extensionRegistryLite) throws com.google.protobuf.InvalidProtocolBufferException {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) com.google.protobuf.GeneratedMessageLite.parseFrom(DEFAULT_INSTANCE, codedInputStream, extensionRegistryLite);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder newBuilder() {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder) DEFAULT_INSTANCE.createBuilder();
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder newBuilder(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage) {
        return (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder) DEFAULT_INSTANCE.createBuilder(qQMessageOuterClass$QQMessage);
    }

    public static final class Builder extends com.google.protobuf.GeneratedMessageLite.Builder<QQMessageOuterClass$QQMessage, Builder> implements top.artmoe.inao.entries.QQMessageOuterClass$QQMessageOrBuilder {
        public Builder() {
            super(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.DEFAULT_INSTANCE);
        }

        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody getMessageBody() {
            return ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) this.instance).getMessageBody();
        }

        public top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder setMessageBody(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.MessageBody messageBody) {
            copyOnWrite();
            ((top.artmoe.inao.entries.QQMessageOuterClass$QQMessage) this.instance).setMessageBody(messageBody);
            return this;
        }
    }

    @Override
    public final java.lang.Object dynamicMethod(com.google.protobuf.GeneratedMessageLite.MethodToInvoke methodToInvoke, java.lang.Object obj, java.lang.Object obj2) {
        int i = top.artmoe.inao.entries.QQMessageOuterClass$1.$SwitchMap$com$google$protobuf$GeneratedMessageLite$MethodToInvoke[methodToInvoke.ordinal()];
        switch (i) {
            case 1:
                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage();
            case 2:
                return new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.Builder();
            case 3:
                return com.google.protobuf.GeneratedMessageLite.newMessageInfo(DEFAULT_INSTANCE, "\u0000\u0003\u0000\u0001\u0001\u0003\u0003\u0000\u0000\u0000\u0001ဉ\u0000\u0002ဉ\u0001\u0003ဉ\u0002", new java.lang.Object[]{"bitField0_", "messageHead_", "messageContentInfo_", "messageBody_"});
            case 4:
                return DEFAULT_INSTANCE;
            case 5:
                com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> parser = PARSER;
                if (parser == null) {
                    synchronized (top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.class) {
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
        top.artmoe.inao.entries.QQMessageOuterClass$QQMessage qQMessageOuterClass$QQMessage = new top.artmoe.inao.entries.QQMessageOuterClass$QQMessage();
        DEFAULT_INSTANCE = qQMessageOuterClass$QQMessage;
        com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(top.artmoe.inao.entries.QQMessageOuterClass$QQMessage.class, qQMessageOuterClass$QQMessage);
    }

    public static top.artmoe.inao.entries.QQMessageOuterClass$QQMessage getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<top.artmoe.inao.entries.QQMessageOuterClass$QQMessage> parser() {
        return DEFAULT_INSTANCE.getParserForType();
    }
}
