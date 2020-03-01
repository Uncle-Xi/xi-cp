package com.xicp.server.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class XiEncoder extends MessageToMessageEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object in, List<Object> list) throws Exception {
		ByteBuf buf = Unpooled.buffer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
		objectOutputStream.writeObject(in);
		objectOutputStream.flush();
		objectOutputStream.close();
		buf.writeBytes(baos.toByteArray());
		list.add(buf);
	}
}
