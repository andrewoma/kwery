create table actor (
  id         integer identity,
  first_name varchar(255) not null,
  last_name  varchar(255) null,
  version    integer      not null
);

create table film (
  id                   integer identity,
  title                varchar(255) not null,
  release_year         integer,
  language_id          integer      not null,
  original_language_id integer,
  length               integer,
  rating               varchar(255),
  special_features     varchar(255) array,
  version              integer      not null
);

create table language (
  id      integer identity,
  name    varchar(255) not null,
  version integer      not null
);

create table film_actor (
  film_id  integer not null,
  actor_id integer not null,
  primary key (film_id, actor_id)
)
