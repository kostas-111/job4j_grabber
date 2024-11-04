
create schema grabber;

create table grabber.post (
				id serial primary key, 
				name varchar(128), 
				text text, 
				link text unique, 
				created timestamp);				

COMMENT ON COLUMN grabber.post.name IS 'Имя вакансии';
COMMENT ON COLUMN grabber.post.text IS 'Текст вакансии';
COMMENT ON COLUMN grabber.post.link IS 'Ссылка на вакансию';
COMMENT ON COLUMN grabber.post.created IS 'Дата публикации';