package br.com.seberino.kafka;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import br.com.seberino.kafka.dto.Pedido;



public class Produtor {

	
	
	public static void main(String[] args) {
		
		int ped=20;
		
		Pedido pedido = new Pedido();
		pedido.setNumero(ped);
		pedido.setValor(40);
		ped++;
		
		Produtor produtor = new Produtor("pedidos");
		produtor.eventoPedidoEnviadoParticionador(pedido);
		
		pedido.setNumero(ped);
		pedido.setValor(140);
		produtor.eventoPedidoEnviadoParticionador(pedido);
		ped++;
		
		pedido.setNumero(ped);
		pedido.setValor(35);
		produtor.eventoPedidoEnviadoParticionador(pedido);
		ped++;
		
		pedido.setNumero(ped);
		pedido.setValor(1000);
		produtor.eventoPedidoEnviadoParticionador(pedido);
		ped++;

		pedido.setNumero(ped);
		pedido.setValor(5);
		produtor.eventoPedidoEnviadoParticionador(pedido);
		ped++;

		pedido.setNumero(ped);
		pedido.setValor(6000);
		produtor.eventoPedidoEnviadoParticionador(pedido);
		ped++;
	}

	
	public Produtor(String topico)
	{
		this.topico = topico;
	}
	private String topico;
	
	
	public void eventoPedidoEnviadoNormal(Pedido pedido)
	{
		Properties kafkaProp = new Properties();
		kafkaProp.put("bootstrap.servers", "localhost:9092");
		kafkaProp.put("key.serializer",  "org.apache.kafka.common.serialization.StringSerializer");
		kafkaProp.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
		kafkaProp.put("acks", "1");
		kafkaProp.put("client.id", "Producer1");
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(kafkaProp);
		ProducerRecord<String, String> record;
		Random rand = new Random();
		int particao=3;
		if (pedido.getValor()>50)
		{
			particao = rand.nextInt(3);
		}
		try
		{
			record = new ProducerRecord<String, String>(this.topico, particao, pedido.getNumero()+"", pedido.toString());
			producer.send(record);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			producer.close();
		}
	}
	
	public void eventoPedidoEnviadoParticionador(Pedido pedido)
	{
		Properties kafkaProp = new Properties();
		kafkaProp.put("bootstrap.servers", "localhost:9092");
		kafkaProp.put("key.serializer",  "org.apache.kafka.common.serialization.StringSerializer");
		kafkaProp.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
		kafkaProp.put("acks", "1");
		kafkaProp.put("client.id", "Producer1");
		kafkaProp.put("partitioner.class", "br.com.seberino.kafka.ParticionadorCustomizado");
		kafkaProp.put("particionador.valorMaximo",50D);
		
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(kafkaProp);
		ProducerRecord<String, String> record;
		try
		{
			record = new ProducerRecord<String, String>(this.topico, pedido.getNumero()+"", pedido.toString());
			producer.send(record);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			producer.close();
		}

	}
	
}

