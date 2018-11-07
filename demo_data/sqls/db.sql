# creating database
create schema grwa_db;


# user tables for authentication

# all users
CREATE TABLE cmn_user (
  id int(11) NOT NULL AUTO_INCREMENT,
  user_code varchar(100) DEFAULT NULL,
  full_name varchar(100) DEFAULT NULL,
  company_name varchar(100) DEFAULT NULL,
  email_address varchar(100) DEFAULT NULL,
  statu varchar(1) DEFAULT NULL,
  user_password varchar(50) DEFAULT NULL,
  user_type varchar(1) DEFAULT NULL,
  update_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY `ix_user_code` (user_code)
) ENGINE=InnoDB AUTO_INCREMENT=563 DEFAULT CHARSET=utf8;

# user roles
CREATE TABLE cmn_user_role (
  user_code varchar(50) NOT NULL,
  role varchar(50) NOT NULL,
  PRIMARY KEY (user_code,role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# creating all other tables

# all indicator definitions from any data source are here.
create table cmn_indicator (
_name text not null,
_code varchar(48) primary key,
description text,
api_code text,
_type varchar(3) not null,
source_code varchar(2) not null,
unit text
);

create table cmn_wb_indicator (
_name text not null,
_code varchar(48) primary key,
unit text,
_type varchar(3) not null
);

create table cmn_fred_indicator (
_name text not null,
_code varchar(48) primary key,
unit text,
_type varchar(2)
);

create table cmn_fred_other_indicator (
_name text not null,
_code varchar(48) primary key,
unit text
);

create table cmn_pr_indicator (
_name text not null,
_code varchar(48) primary key,
unit text
);

create table cmn_fe_indicator (
_name text not null,
_code varchar(48) primary key,
unit text
);

# countries, aggragates, fixed or changing variables all are defined here
create table cmn_region (
_code varchar(2) PRIMARY KEY, 
_name text not null, 
description text,
type_code varchar(3), 
code_3 varchar(3) UNIQUE, 
currency text,
currency_code varchar(3), 
landlocked bool, 
distance int,

# group code comes from first given core country data
group_code varchar(6),
sub_continent_id int(3),
legal_origin_code varchar(2),

# region code and admin region codes come from WB
region_code varchar(2),
admin_group_code varchar(2),
income_code varchar(2),
capital text,
lending_code varchar(2)
);

# unique risk factor table for any combination
create table risk_factor(
_id int PRIMARY KEY auto_increment, 
region_code varchar(6) not null, 
indicator_code varchar(48) not null,
source_code varchar(2) not null,
url text not null,
_type varchar(3) not null,
annual boolean not null default 0,
quarterly boolean not null default 0,
monthly boolean not null default 0,
annual_update_date date not null,
quarterly_update_date date not null,
monthly_update_date date not null,

unique(region_code, indicator_code)
);

create table rf_group(
_id int PRIMARY KEY, 
country_code varchar(2) not null, 
group_code varchar(6) not null,
update_date date not null,
unique(country_code, group_code)
);

# unique historical data table for any risk factor including dynamic country groups
create table prc_value(
data_date DATE not null, 
risk_factor_id int not null, 
price DOUBLE not null,
frequency_code varchar(2) not null,
unique(data_date, risk_factor_id, frequency_code)
);

create table prc_group(
data_date DATE not null,
country_code varchar(2) not null,
group_code varchar(6) not null,
unique(data_date, country_code, group_code)
);

create table cmn_geo_group (
_code varchar(6) primary key,
_name text
);


create table cmn_sub_continent (
_id int(3) primary key,
_name text not null,
continent_id int(3) not null
);

create table cmn_continent(
_id int(3) primary key,
_name text not null
);

create table cmn_frequency(
_code varchar(2) primary key,
_name text not null
);

create table cmn_legal_origin(
_code varchar(2) primary key,
_name text
);

create table cmn_dynamic_group(
_code varchar(6) primary key,
_name text not null,
parent_code varchar(3) not null
);

create table cmn_group(
_code varchar(6) primary key,
_name text not null,
parent_code varchar(3) not null
);

create table cmn_parent_group(
_code varchar(3) primary key,
_name text not null
);

create table save(
_name varchar(10) primary key,
last_index text not null
);

create table cmn_data_source(
_name text not null,
_code varchar(2) primary key,
priority float
);