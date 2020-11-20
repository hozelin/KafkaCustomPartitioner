package br.com.seberino.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;


public class ConsumidorV1 {

	public static void main(String[] args)
	{
		ConsumidorV1 con = new ConsumidorV1("pedidos");
		con.consumidor();
	}
	
	private String topico;
	public ConsumidorV1(String topico)
	{
		this.topico = topico;
	}
	
	public void consumidor()
	{
		Properties kafkaProp = new Properties();
		kafkaProp.put("bootstrap.servers", "localhost:9092");
		kafkaProp.put("key.deserializer",  "org.apache.kafka.common.serialization.StringDeserializer");
		kafkaProp.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
		kafkaProp.put("group.id","cg1");
		kafkaProp.put("cliente.id","V1");
		
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(kafkaProp);
		
		try
		{
			ArrayList<TopicPartition> lista = new ArrayList<TopicPartition>();
			lista.add(new TopicPartition(this.topico, 0));
			lista.add(new TopicPartition(this.topico, 1));
			lista.add(new TopicPartition(this.topico, 2));

			consumer.assign(lista);
			while (true)
			{
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<String, String> record: records)
				{
					System.out.println("Evento recebido topic " + record.topic() + ", partion = " + record.partition() 
					+ ", offset " + record.offset() + ", chave " + record.key() + ", evento " + record.value());
				}
				consumer.commitSync();
				
			}

		}
		catch (Exception e)
		{
			System.out.println("Falha na leitura");
			e.printStackTrace();
		}
		finally
		{
			consumer.close();
		}

	}

}
