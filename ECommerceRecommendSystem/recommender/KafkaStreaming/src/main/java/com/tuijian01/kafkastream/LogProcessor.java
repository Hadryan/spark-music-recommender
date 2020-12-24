package com.tuijian01.kafkastream;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

public class LogProcessor implements Processor<byte[], byte[]> {

    private ProcessorContext context;

    //初始化
    @Override
    public void init(ProcessorContext processorContext) {
        this.context = processorContext;
    }

    @Override
    public void process(byte[] dummy, byte[] line) {
        //核心处理流程
        String input = new String(line);
        //进行过滤和提取数据，以固定的前缀过滤日志信息
        if (input.contains("PRODUCT_RATING_PREFIX:")) {
            System.out.println("商品评分数据来了！>>>>>>>>>>>>" + input);
            input = input.split("PRODUCT_RATING_PREFIX:")[1].trim();
            context.forward("logProcessor".getBytes(),input.getBytes());
        }

    }

    @Override
    public void punctuate(long l) {

    }

    @Override
    public void close() {

    }
}
