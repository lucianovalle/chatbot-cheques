create table taxas (
    id bigint not null auto_increment,
    modalidade varchar(60) not null,
    taxa_diaria decimal(10, 6) not null,

    primary key (id),
    constraint uk_taxas_modalidade unique (modalidade)
) engine=InnoDB default charset=utf8mb4;
