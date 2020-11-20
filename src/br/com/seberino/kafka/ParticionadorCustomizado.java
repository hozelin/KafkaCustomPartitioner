package br.com.seberino.kafka;

import java.util.Map;
import java.util.Random;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import br.com.seberino.kafka.dto.Pedido;

public class ParticionadorCustomizado implements Partitioner {
	private double valor=0D;
	@Override
	public void configure(Map<String, ?> configs) {
		if (configs!=null)
		{
			this.valor =Double.parseDouble(configs.get("particionador.valorMaximo").toString());
		}
	}
	@Override
	public int partition(String topico, Object key, byte[] keyBytes, Object value, byte[] valueBytes , Cluster cluster) 
	{
		int particao = 0;
		try
		{
			Pedido pedido = Pedido.getPedido(value.toString());
			if (pedido.getValor()>valor)
			{
				particao = rand.nextInt(3);
			}
			else
			{
				particao=3;	
			}
			
		} catch (Exception e){ }
		return particao;	
	}

	@Override
	public void close() {
		
	}
	private Random rand = new Random();

}
