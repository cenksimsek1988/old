create table cmn_wb_indicator (
_name text not null,
_code varchar(48) primary key,
description text,
api_code text,
_type varchar(3) not null,
source_code varchar(2) not null
);


create table cmn_wb_region (
_code varchar(2) PRIMARY KEY, 
_name text not null, 
description text,
type_code varchar(3), 
code_3 varchar(3) UNIQUE, 
currency text,
currency_code varchar(3), 
landlocked bool, 
distance int, 
region_code varchar(6),
sub_continent_id int(3),
legal_origin_code varchar(2),
group_code varchar(2),
admin_group_code varchar(2),
income_code varchar(2),
capital text,
lending_code varchar(2)
);