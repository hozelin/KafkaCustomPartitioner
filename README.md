# Canary Release e Teste A/B com aplicações baseadas em eventos

O termo da moda em desenvolvimento é o da arquitetura de aplicações baseadas em eventos, o termo em inglês se você preferir: Event Driven Architecture. Existe por ai muita literatura sobre o tema, desde como modelar seus eventos, discussões sobre vazamentos de contexto, adoção de schemas de forma eficiênte, alguns patterns interessantes como CQRS, Event Sourcing e Sagas.  Porém existem alguns aspectos desta adoção que ainda foi pouco explorado, e nos projetos em que estou envolvido tive a necessidade de pesquisar, trocar figurinhas com alguns colegas e  fazer algumas experiementações para chegar em alguns modelos de deployment que habilitam os times a experimentarem novas features e versões de aplicações baseadas em eventos em um grupo controlado de usuários. Isso mesmo, estamos falando de Canary Release e Test A/B.

O propósito desde repositório é explorar algumas das alterantivas para usar os conceitos de "Canary Deployment" e Teste A/B em aplicações baseadas em eventos e microserviços.

## Conceituando:
Canary Release:  O termos nos remete aos trabalhos de exploração de minérios em minas. Quando as equipes atingiam determinadas regiões das minas existiam o risco de terem gazes venenosos. Para proteger os trabalhadores deste risco, a mina era esvaziada e soltada naquela região um canário, se o mesmo voltasse depois de um tempo era sinal que o lugar não estava contamido, porém se ele não voltasse significava que ninguém deveria se aproximar por alguns dias, até que novo teste fosse feito pois o perigo era eminente. No mundo de software o termo é empregado quando queremos testar uma versão nova da nossa aplicação em um grupo restrito de usuários de forma controlada, uma espécie de piloto. No final o propósito é o mesmo, se nenhum usuário que esteja participando do piloto for impacto por um período de tempo aquela nova versão é liberada para os demais, caso algum usuário seja impactado, pelo menos o impacto é restrito aquele grupo menor.  

Teste A/B: esta técnica é usada quando liberamos duas versões diferentes de uma mesma aplicação para dois grupos de usuários e depois comparamos o resultado. Pode ser coisas simples como mudar a cor de fundo do seu portal de e-commerce e observar se os usuários com a cor padrão compraram mais ou menos em um período do que os usuários expostos a cor diferente. 

## Caso de Uso
Para aplicar esses dois conceitos existem muitos recursos para o mundo de microserviços baseados em REST, porém pouca informação de como aplicar essas mesmas técnicas em aplicações que reagem a eventos. No nosso caso de uso teremos uma aplicação de exemplo que produz eventos em um mediador de eventos, o Apache Kafka. Teremos uma aplicação que reage, ou seja, consome esses eventos. Em algum momento vamos liberar uma versão nova do nosso consumidor que irá ser executada junto com a versão anterior para consumir os eventos, e vamos direcionar um grupo reduzido de mensagens para esta nova versão para ter certeza que esta tudo bem antes de substituir definitivamente a versão atual pela nova para todos os eventos produzidos. 

Para isso usaremos o recurso das partições. Onde dedicaremos um conjunto de partições para atender a versão atual do consumidor, a V1, e uma partição que receberá somente os eventos que serão processados pela versão mais nova da aplicação, a V2.

Aqui no exemplo escolhi o critério do valor do pedido. A aplicação que produz os eventos, dependendo do valor do pedido, maiores que R$ 50,00, enviará o evento para o grupo de partições atendidas pela versão V1 do consumidor e menores ou igual a R$ 50,00 para a partição 3 do tópico que é atendida apenas para a versão nova, a V2 do consumidor. 
 ![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/diagrama01.png)

Para entender como essa solução funciona, precisamos primeiro entender como o Kafka distribui nativamente as mensagens dentro das partições de um tópico na gravação e como ele distribui essas partições entre os consumidores.

#Vocabulário:
Tópico: é a referência lógica de onde vamos produzir nossos eventos no mediador. Fazendo uma analogia ao mundo dos bancos de ddos é como se o tópico fosse nossa tabela. 
Partição: cada tópico pode ser dividido em 1 ou mais partições. A partição é o mecanismo que o Kafka usa para escalar a escrita e gravação nos tópicos. Quanto mais partições mais aplicações serão capazes de consumir eventos em paralelo do mesmo tópico.
Partitioner: é o componente da API Kafka do produtor que é quem decide para qual partição uma mensagem será enviada. 

## Produtor

Agora que entendemos como a API do Kafka trabalha para distribuir mensagens nas partições vamos as possíveis soluções.
1.	O produtor no momento de criar a mensagem já informa em qual partição quer gravar o evento. 
![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/produtornormal.png)

Neste exemplo a mágica esta nas linhas 73,74,75,76,77.. Ali usamos uma classe do java para de forma randômica escolher uma partição entre 0 e 2 caso o valor do pedido seja maior que 50, caso contrário o evento será gravado na partição 3.

Perceba na linha 81 que no momento da criação do registro um parâmetro a mais, que é opcional, é informado dizendo o rKafka em qual partição você deseja gravar o evento. Para isso usamos o código anterior para decidir. 

2.	Outra alternativa um pouco mais elegante é usar um particionador customizado que tenha essa regra do particionamento. Para isto basta criar uma classe que implemente a interface org.apache.kafka.clients.producer.Partitioner conforme o exemplo abaixo:

![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/produtor2.png)

Dois pontos importantes, primeiro o método configure que recebe os parâmetros do cliente, coloquei na linha 17 um código para procurar por uma configuração nova que eu mesmo defini, onde o usuário vai configurar até que valor o pedido será enviado para a partição 3 (onde será atendido pela aplicação na versão mais nova). 

No método "partition" que começa na linha temos a regra de negócio que estava explicita no Produtor e que agora será processada aqui, ela é bem simples e muito parecida com a do exemplo 1, caso o pedido seja maior que o valor configurado, ele escolherá de forma aleatória uma partição entre 0 e 2, caso contrário irá para a partição 3. 

Neste modelo o Produtor fica mais simples, e exige duas configurações adicionais. 

![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/produtor2config.png)

Na linha 102, temos que informar qual é a classe que será responsável por definir em qual partição o evento será gravado. Na linha 103 temos a parâmetro novo que criamos que será usado para tomar a decisão pelo lado do particionador.

A criação do registro desta vez, que ocorre na linha 109, não precisa mais informar o número da partição o que deixa o código aqui mais simples e a inteligência pode ser reaproveitada em outros tópicos. 

## Consumidor
Independente da abordagem que você usou, ou a primeira ou a segunda, temos agora um produtor que respeitando um critério de negócio, no nosso caso valor do pedido, separa determinados eventos em uma partição separada para ser usada por uma versão nova geralmente em teste da aplicação. 

A versão V1 do consumidor seria como o exemplo abaixo:
![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/consumidorv1.png)

Entre as linhas 40 e 45 está o segredo, primeiro criamos um array com a lista de partições que esta aplicação consumir eventos, neste caso omitimos a partição 3 que será dedicada a eventos para a versão V2 da nossa aplicação. Outro ponto que vale a menção ocorre na linha 45 é que ao invés de fazermos o "subscribe" e deixar que o kafka nos diga quais partições nossa aplicação vai ouvir, usamos o método "assign" já com a lista pre-definida. 

A aplicação V2 é exatamente igual, porém escuta apenas os eventos gravados na partição 3 do tópico. 
![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/consumidorv2.png)

## Perparando nosso ambiente:
### Pré-requisitos:
- Docker instalado (para rodar o Kafka)
- 	Java JDK 1.8 ou superior instalado
- 	Cliente do GitHub instalado para baixar o conteúdo desse repositório.

### 1 - Kafka
Cria uma instancia do Kafka executando o comando abaixo:

`docker run -d -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 -p 9581-9585:9581-9585 -p 9092:9092 --name=kafka  -e ADV_HOST=127.0.0.1 landoop/fast-data-dev:latest`

Agora crie um tópico chamado pedidos com 4 partições usando o comando abaixo:
 
`docker exec -ti kafka bash -c "/opt/landoop/kafka/bin/kafka-topics --create --bootstrap-server localhost:9092 --topic pedidos --partitions 4 --config retention.ms=120000"`

### 2 - Execute os dois consumidores: V1 e V2
Compile o código e execute as classes "ConsumidorV1" e "ConsumidorV2" pela sua IDE ou via linha de comando

### 3 - Execute o produtor
Compile o código e execute a classe "Produtor" pela sua IDE ou via linha de comando para gerar alguns registros para o nosso teste

### 4 - Verifique o resultado
•	Nos logs da aplicação V1 só devem ter mensagens com mais de R$ 50,00
![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/logconv1.png)
•	Nos logs da aplicação V2 só devem ter mensagens com valor menor ou igual R$ 50,00
![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/logconv2.png)
•	Pela console do Kafka (http://localhost:3030) é possível ver as mensagens distribuídas por partição
 ![alt text](https://github.com/richardseberino/KafkaCustomPartitioner/blob/main/images/kafkaconsole.png)


## Conclusão
Vimos aqui com podemos implementar conceitos para termos de forma concorrente duas versões diferentes de um mesmo micro serviço baseado em eventos trabalhando em paralelo consumindo eventos do mesmo tópico mas segregados por algum critério de negócio. 

Em um exemplo real tenha mais partições para seu tópico e reserve um percentual delas para estratégias de canary ou teste A/B. 

