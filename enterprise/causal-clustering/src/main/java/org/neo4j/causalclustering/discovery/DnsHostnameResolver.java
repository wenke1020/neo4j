/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.causalclustering.discovery;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

import static java.lang.String.format;

public class DnsHostnameResolver extends RetryingHostnameResolver
{
    private final Log userLog;
    private final Log log;
    private final DomainNameResolver domainNameResolver;

    public static DnsHostnameResolver getInstance( LogProvider logProvider, LogProvider userLogProvider, DomainNameResolver domainNameResolver, Config config )
    {
        return new DnsHostnameResolver( logProvider, userLogProvider, domainNameResolver, config, defaultRetryStrategy( config, logProvider ) );
    }

    DnsHostnameResolver( LogProvider logProvider, LogProvider userLogProvider, DomainNameResolver domainNameResolver, Config config,
            MultiRetryStrategy<AdvertisedSocketAddress,Collection<AdvertisedSocketAddress>> retryStrategy )
    {
        super( config, retryStrategy );
        log = logProvider.getLog( getClass() );
        userLog = userLogProvider.getLog( getClass() );
        this.domainNameResolver = domainNameResolver;
    }

    @Override
    protected Collection<AdvertisedSocketAddress> resolveOnce( AdvertisedSocketAddress initialAddress )
    {
        Set<AdvertisedSocketAddress> addresses = new HashSet<>();
        InetAddress[] ipAddresses;
        ipAddresses = domainNameResolver.resolveDomainName( initialAddress.getHostname() );
        if ( ipAddresses.length == 0 )
        {
            log.error( "Failed to resolve host '%s'", initialAddress.getHostname() );
        }

        for ( InetAddress ipAddress : ipAddresses )
        {
            addresses.add( new AdvertisedSocketAddress( ipAddress.getHostAddress(), initialAddress.getPort() ) );
        }

        userLog.info( "Resolved initial host '%s' to %s", initialAddress, addresses );
        return addresses;
    }

}
