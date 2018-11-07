create table cmn_fred_us_indicator (
indicator_name text not null,
indicator_code varchar(48) primary key,
indicator_description text,
indicator_api_code text,
indicator_type varchar(3) not null,
indicator_data_source_code varchar(2) not null
);

create table cmn_fred_other_indicator (
_name text not null,
_code varchar(48) primary key,
description text,
api_code text,
_type varchar(3) not null,
source_code varchar(2) not null
);


create table cmn_fred_region (
region_code varchar(2) PRIMARY KEY, 
region_name text not null, 
region_description text,
region_type_code varchar(3), 
region_code_3 varchar(3) UNIQUE, 
country_currency text,
country_currency_code varchar(3), 
country_landlocked bool, 
country_distance_port int, 
country_region_code varchar(6),
country_sub_continent_id int(3),
country_legal_origin_code varchar(2),
country_group_code varchar(2),
country_admin_group_code varchar(2),
country_income_code varchar(2),
country_capital text,
country_lending_code varchar(2)
);