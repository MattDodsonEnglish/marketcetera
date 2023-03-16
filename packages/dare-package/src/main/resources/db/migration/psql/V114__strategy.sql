--
-- Name: metc_strategy_instances; Type: TABLE; Schema: public; Owner: metc
--

CREATE TABLE metc_strategy_instances (
    id bigint NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    update_count integer NOT NULL,
    filename character varying(255),
    hash character varying(255),
    name character varying(255),
    nonce character varying(255),
    started timestamp without time zone,
    status character varying(255),
    user_id bigint
);

--
-- Name: metc_strategy_instances metc_strategy_instances_pkey; Type: CONSTRAINT; Schema: public; Owner: metc
--

ALTER TABLE ONLY metc_strategy_instances
    ADD CONSTRAINT metc_strategy_instances_pkey PRIMARY KEY (id);


--
-- Name: metc_strategy_instances fkdd09qewndxtdyqxybydxr6exr; Type: FK CONSTRAINT; Schema: public; Owner: metc
--

ALTER TABLE ONLY metc_strategy_instances
    ADD CONSTRAINT fkdd09qewndxtdyqxybydxr6exr FOREIGN KEY (user_id) REFERENCES metc_users(id);

insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to read loaded strategies','ReadStrategyAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to load strategies','LoadStrategyAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to unload strategies','UnloadStrategyAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to start loaded strategies','StartStrategyAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to stop started strategies','StopStrategyAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Remove events generated by strategies','ClearStrategyEventsAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to cancel strategies being uploaded','CancelStrategyUploadAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to read strategy messages','ReadStrategyMessagesAction');
insert into metc_permissions values(((select max(id) from metc_permissions)+1),now(),0,'Access to rcreate strategy messages','CreateStrategyMessagesAction');
insert into metc_roles_permissions select a.id as roles_id,b.id as permissions_id from metc_roles a,metc_permissions b where b.name like '%Strategy%Action' and (a.name='Trader' or a.name='TraderAdmin');

--
-- Name: metc_strategy_messages; Type: TABLE; Schema: public; Owner: metc
--

CREATE TABLE metc_strategy_messages (
    id bigint NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    update_count integer NOT NULL,
    message character varying(255),
    message_timestamp timestamp without time zone,
    severity integer,
    strategy_instance_id bigint
);

--
-- Name: metc_strategy_messages metc_strategy_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: metc
--

ALTER TABLE ONLY metc_strategy_messages
    ADD CONSTRAINT metc_strategy_messages_pkey PRIMARY KEY (id);


--
-- Name: metc_strategy_messages fk66jhynr1a7r5x55l5c0x0hasx; Type: FK CONSTRAINT; Schema: public; Owner: metc
--

ALTER TABLE ONLY metc_strategy_messages
    ADD CONSTRAINT fk66jhynr1a7r5x55l5c0x0hasx FOREIGN KEY (strategy_instance_id) REFERENCES metc_strategy_instances(id);
