package br.com.seberino.teste;

import br.com.seberino.kafka.Produtor;
import br.com.seberino.kafka.dto.Pedido;

public class Teste {

	public static void main(String[] args) {
		
		Pedido pedido = new Pedido();
		pedido.setNumero(10);
		pedido.setValor(40);
		
		Produtor produtor = new Produtor("pedidos");
		produtor.eventoPedidoEnviadoNormal(pedido);
		

	}

}
