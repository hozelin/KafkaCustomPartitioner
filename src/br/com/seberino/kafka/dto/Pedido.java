package br.com.seberino.kafka.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Pedido {

	public Pedido()
	{
		
	}
	
	public static Pedido getPedido(String json) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		Pedido retorno = mapper.readValue(json, Pedido.class);
		return retorno;
	}
	
	private int numero;
	private double valor;
	
	public int getNumero() {
		return numero;
	}
	public void setNumero(int numero) {
		this.numero = numero;
	}
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	
	@Override
	public String toString() {
		StringBuffer pp = new StringBuffer();
		pp.append("{ \"numero\": " + this.numero+ ", \"valor\": " + this.valor + "} ");
		return pp.toString();
	}
	
	
}
