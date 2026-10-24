create table boleto (
    id bigint not null auto_increment,
    numero varchar(20) not null,
    valor decimal(15, 2) not null,
    data_vencimento date not null,

    primary key (id),
    constraint uk_boleto_numero unique (numero)
) engine=InnoDB default charset=utf8mb4;
