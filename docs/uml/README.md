# 📚 Central de Modelagem UML & Arquitetura

> **Autor:** Marcel Florentino ([@applephoria](https://github.com/applephoria))  
> **Padrão:** OMG Unified Modeling Language (UML 2.5)

Bem-vindo à central de modelagem técnica do **TransactCore-Java**. Este espaço detalha os diagramas de engenharia que descrevem a estrutura de tipos, as interações temporais e o particionamento em camadas do motor transacional.

---

## 🗺️ Mapa dos Diagramas

| Documento | Tipo de Diagrama | Foco de Engenharia |
| :--- | :---: | :--- |
| [📐 Diagrama de Classes](./diagrama_classes.md) | **Estrutural** | Classes, interfaces, métodos, tipos de retorno e relações de dependência. |
| [⏱️ Diagrama de Sequência](./diagrama_sequencia.md) | **Comportamental** | Ciclo de vida transacional ACID, chamadas ao socket JDBC, `commit` e `rollback`. |
| [🏛️ Arquitetura em Camadas](./arquitetura_camadas.md) | **Arquitetural** | Separação de responsabilidades (Clean Architecture) e fluxo de dados unidirecional. |

---

## 💡 Princípios Aplicados

* **Inversão de Dependência (DIP / SOLID):** A camada de serviço depende de interfaces (`ContaRepository`, `TransferenciaRepository`) e nunca de implementações concretas acopladas.
* **Responsabilidade Única (SRP / SOLID):** Repositórios cuidam exclusivamente de SQL; Entidades cuidam exclusivamente de invariantes e saldos; Serviços orquestram o ciclo transacional.
* **Programação Defensiva (Fail-Fast):** Todas as entradas são validadas na fronteira imediata antes de alocar recursos caros ou disparar I/O de rede.
